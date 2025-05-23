import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useEffect, useState } from 'react'; // React import for completeness
import { Alert, FlatList, Image, RefreshControl, StyleSheet, TouchableOpacity, View } from 'react-native';
import {
    Button,
    Card,
    Chip,
    Divider,
    ActivityIndicator as PaperActivityIndicator,
    Paragraph,
    Text,
    Title,
    useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const API_BASE_URL = 'http://10.7.41.239:8080'; // <<< CORRECTED: Added http://

// AdminReviewScreen receives currentUser as a direct prop from App.js navigator
export default function AdminReviewScreen({ navigation, route, currentUser }) { // <<< CORRECTED: currentUser is now a direct prop
    const { colors } = useTheme();
    const [claimsForReview, setClaimsForReview] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState({ type: null, claimId: null });

    // REMOVED: const { currentUser } = route.params || {}; 
    // We use the currentUser passed as a direct prop.

    useEffect(() => {
        console.log("[AdminReviewScreen] Received currentUser prop in useEffect:", JSON.stringify(currentUser, null, 2));
        if (!currentUser || !currentUser.id || currentUser.role !== 'admin') {
            Alert.alert("Access Denied", "You do not have permission to view this page or user data is not fully loaded. Please log in as an admin.");
            // if (navigation.canGoBack()) {
            //     navigation.goBack();
            // }
        }
    }, [currentUser, navigation]);


    const fetchAdminReviewClaims = useCallback(async () => {
        if (!currentUser || currentUser.role !== 'admin' || !currentUser.id) {
            console.log("[AdminReviewScreen] fetchAdminReviewClaims: Not an admin or currentUser is invalid/not yet available. Skipping fetch.");
            setClaimsForReview([]); 
            setIsLoading(false); 
            return;
        }

        let isActive = true; 
        console.log("[AdminReviewScreen] Fetching claims for admin review (Admin User ID: " + currentUser.id + ")");
        setIsLoading(true);
        try {
            const response = await fetch(`${API_BASE_URL}/api/claims/admin-review`, {
                // TODO: headers: { 'Authorization': `Bearer YOUR_ADMIN_TOKEN` } 
            });

            if (!isActive) return;

            if (!response.ok) {
                let errorMsg = `Failed to fetch claims (status: ${response.status})`;
                try {
                    const errData = await response.json();
                    errorMsg = errData.error || errData.message || errorMsg;
                } catch (e) {
                    const textError = await response.text().catch(() => "Server returned an unreadable error or no content.");
                    errorMsg = textError || errorMsg;
                }
                console.error("Error fetching admin review claims - Response not OK:", errorMsg);
                Alert.alert("Error Loading Claims", errorMsg);
                if (isActive) setClaimsForReview([]);
                return; 
            }

            const data = await response.json();
            if (isActive) {
                setClaimsForReview(Array.isArray(data) ? data : []);
                console.log(`[AdminReviewScreen] Fetched ${Array.isArray(data) ? data.length : 0} claims for review.`);
            }
        } catch (error) {
            console.error("Error fetching admin review claims (catch block):", error);
            if (isActive) {
                Alert.alert("Error Loading Claims", "Could not load claims for review. " + error.message);
                setClaimsForReview([]);
            }
        } finally {
            if (isActive) setIsLoading(false);
        }
    }, [currentUser]); 

    useFocusEffect(
        useCallback(() => {
            console.log("[AdminReviewScreen] useFocusEffect triggered.");
            async function fetchDataOnFocus() {
                await fetchAdminReviewClaims();
            }
            fetchDataOnFocus(); 
            return () => {}; 
        }, [fetchAdminReviewClaims]) 
    );

    const handleAdminAction = async (claimId, actionType, reason = null) => {
        console.log("[AdminReviewScreen] handleAdminAction called with currentUser:", JSON.stringify(currentUser, null, 2));
        if (!currentUser || currentUser.role !== 'admin' || !currentUser.id) {
            Alert.alert("Unauthorized", "Only admins can perform this action. Ensure you are logged in as an admin and user data is loaded.");
            return;
        }
        const endpointAction = actionType === 'approve' ? 'approve' : 'reject';
        setActionLoading({ type: actionType, claimId });

        try {
            const bodyPayload = { 
                userId: currentUser.id 
            };
            if (actionType === 'reject' && reason) {
                bodyPayload.rejectionReason = reason;
            }

            const response = await fetch(`${API_BASE_URL}/api/claims/${claimId}/${endpointAction}`, {
                method: 'PUT',
                headers: { 
                    'Content-Type': 'application/json',
                    // TODO: 'Authorization': `Bearer YOUR_ADMIN_TOKEN` 
                },
                body: JSON.stringify(bodyPayload)
            });

            const responseData = await response.json().catch(() => null);

            if (response.ok) {
                Alert.alert(
                    `Claim ${actionType === 'approve' ? 'Approved' : 'Rejected'}`,
                    responseData?.message || `Claim ${claimId} has been ${actionType === 'approve' ? 'approved' : 'rejected'}.`
                );
                fetchAdminReviewClaims(); 
            } else {
                throw new Error(responseData?.error || responseData?.message || `Failed to ${actionType} claim.`);
            }
        } catch (error) {
            Alert.alert(`Error ${actionType === 'approve' ? 'Approving' : 'Rejecting'} Claim`, error.message);
        } finally {
            setActionLoading({ type: null, claimId: null });
        }
    };

    const promptAndRejectClaim = (claimId) => {
        Alert.prompt("Reject Claim", "Admin: Optional reason for rejection.",
            [
                { text: "Cancel", style: "cancel", onPress: () => {} },
                { text: "Confirm Rejection", onPress: (reason) => handleAdminAction(claimId, 'reject', reason || "Rejected by admin.") }
            ],
            "plain-text"
        );
    };

    const renderClaimItem = ({ item: claim }) => (
        <Card style={styles.claimCard}>
            <Card.Content>
                <Title style={styles.cardTitle}><Text>Claim for Item ID: {claim.itemId}</Text></Title>
                <Paragraph><Text>Claimant: {claim.username || 'N/A'} (User ID: {claim.userId})</Text></Paragraph>
                <Paragraph style={styles.claimDate}><Text>Date: {new Date(claim.claimDate).toLocaleString()}</Text></Paragraph>
                <Divider style={styles.divider} />
                <Text style={styles.detailLabel}>Proof Description:</Text>
                <Paragraph style={styles.descriptionText}>{claim.description}</Paragraph>
                {claim.proofImagePath && (
                    <View style={styles.proofImageContainer}>
                        <Text style={styles.proofImageLabel}>Proof Image:</Text>
                        <TouchableOpacity onPress={() => Alert.alert("Proof Image", "Implement full screen image view or zoom.")}>
                            <Image source={{ uri: `${API_BASE_URL}/api/items/images/${claim.proofImagePath}` }} style={styles.proofImage} resizeMode="contain" />
                        </TouchableOpacity>
                    </View>
                )}
                <Chip 
                    icon="information-outline" 
                    style={[styles.statusChipInternal, {backgroundColor: claim.status === 'FORWARDED_TO_ADMIN' ? colors.tertiaryContainer : colors.surfaceVariant }]} 
                    textStyle={{color: claim.status === 'FORWARDED_TO_ADMIN' ? colors.onTertiaryContainer : colors.onSurfaceVariant}}
                >
                    <Text>Status: {claim.status}</Text>
                </Chip>
            </Card.Content>
            {claim.status === 'FORWARDED_TO_ADMIN' && (
                <Card.Actions style={styles.actions}>
                    <Button 
                        mode="outlined" 
                        onPress={() => promptAndRejectClaim(claim.id)} 
                        textColor={colors.error} 
                        style={[styles.actionButton, {borderColor: colors.error}]}
                        loading={actionLoading.type === 'reject' && actionLoading.claimId === claim.id}
                        disabled={actionLoading.claimId !== null && (actionLoading.claimId !== claim.id || actionLoading.type !== 'reject')}
                        icon="close-circle-outline"
                        compact
                    ><Text>Reject</Text></Button>
                    <Button 
                        mode="contained" 
                        onPress={() => handleAdminAction(claim.id, 'approve')} 
                        style={[styles.actionButton, {backgroundColor: colors.primary}]}
                        loading={actionLoading.type === 'approve' && actionLoading.claimId === claim.id}
                        disabled={actionLoading.claimId !== null && (actionLoading.claimId !== claim.id || actionLoading.type !== 'approve')}
                        icon="check-circle-outline"
                        compact
                    ><Text>Approve</Text></Button>
                </Card.Actions>
            )}
        </Card>
    );

    if (!currentUser || !currentUser.id) { 
        console.log("[AdminReviewScreen] currentUser is null or has no id, showing loader/message.");
        return (
            <View style={styles.loaderContainer}>
                <PaperActivityIndicator size="large" animating={true} />
                <Text style={{marginTop:10, color: colors.onSurfaceVariant}}>Loading Admin Data...</Text>
            </View>
        );
    }
    if (currentUser.role !== 'admin') {
        console.log("[AdminReviewScreen] currentUser is not admin. Role:", currentUser.role);
         return (
            <View style={styles.loaderContainer}>
                <Icon name="alert-octagon-outline" size={48} color={colors.error} />
                <Text style={{marginTop:10, color: colors.error, fontSize: 16, textAlign: 'center'}}>Access Denied. You do not have admin privileges.</Text>
                 <Button onPress={() => navigation.canGoBack() ? navigation.goBack() : navigation.replace('Login')} style={{marginTop: 20}}><Text>Go Back</Text></Button>
            </View>
        );
    }

    if (isLoading && claimsForReview.length === 0) {
        return <View style={styles.loaderContainer}><PaperActivityIndicator size="large" animating={true} /><Text style={{marginTop:10, color: colors.onSurfaceVariant}}>Loading claims for review...</Text></View>;
    }

    return (
        <FlatList
            data={claimsForReview}
            renderItem={renderClaimItem}
            keyExtractor={(claim) => claim.id.toString()}
            style={styles.container}
            contentContainerStyle={claimsForReview.length === 0 ? styles.emptyContainer : styles.listContentContainer}
            ListEmptyComponent={!isLoading ? (
                <View style={styles.emptyContainerContent}>
                    <Icon name="text-box-check-outline" size={60} color={colors.onSurfaceDisabled} />
                    <Text style={styles.emptyText}>No claims currently awaiting admin review.</Text>
                </View>
            ) : null}
            refreshControl={
                <RefreshControl refreshing={isLoading} onRefresh={fetchAdminReviewClaims} colors={[colors.primary]} tintColor={colors.primary}/>
            }
        />
    );
}

// --- Styles (Same as admin_review_screen_v2_fixes) ---
const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f4f6f8', },
    listContentContainer: { padding: 10, paddingBottom: 20, },
    loaderContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
    claimCard: { marginVertical: 8, marginHorizontal:5, elevation: 3, borderRadius: 10 },
    cardTitle: { fontSize: 17, fontWeight: 'bold', marginBottom: 5, },
    claimDate: { fontSize: 12, color: '#6c757d', marginBottom: 10, },
    divider: { marginVertical: 8, },
    detailLabel: { fontSize: 13, fontWeight: '600', color: '#495057', marginTop: 8, },
    descriptionText: { marginVertical: 5, fontSize: 14, lineHeight: 20, },
    proofImageContainer: { marginTop: 10, alignItems: 'center' },
    proofImageLabel: { fontWeight: 'bold', marginBottom: 5, fontSize: 13, color: '#495057', alignSelf: 'flex-start' },
    proofImage: { width: '100%', height: 200, resizeMode: 'contain', marginTop: 5, borderRadius: 6, backgroundColor: '#f0f0f0', borderWidth: 1, borderColor: '#e0e0e0', },
    statusChipInternal: { alignSelf: 'flex-start', marginTop: 12, paddingHorizontal: 8, },
    actions: { justifyContent: 'space-between', paddingTop: 12, paddingBottom: 8, paddingHorizontal: 8, borderTopWidth: 1, borderTopColor: '#f0f0f0' },
    actionButton: { flex: 1, marginHorizontal: 4, },
    emptyContainer: { flexGrow: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
    emptyContainerContent: { alignItems: 'center'},
    emptyText: { marginTop: 16, fontSize: 16, color: '#6c757d', textAlign: 'center' },
});
