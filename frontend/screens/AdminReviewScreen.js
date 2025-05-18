import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useState } from 'react';
import { Alert, FlatList, Image, RefreshControl, StyleSheet, TouchableOpacity, View } from 'react-native';
import {
    Button,
    Card,
    Chip,
    Divider,
    ActivityIndicator as PaperActivityIndicator,
    Paragraph,
    Text,
    Title, // Correctly imported
    useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const API_BASE_URL = 'http://10.7.41.147:8080'; // Ensure this is correct

export default function AdminReviewScreen({ route, navigation }) { // Ensure currentUser is a prop
  console.log("[AdminReviewScreen] Received currentUser prop:", JSON.stringify(currentUser, null, 2));
  // ...
    const { currentUser } = route.params || {}; // Assuming currentUser (with role and id) is passed
    const { colors } = useTheme();
    const [claimsForReview, setClaimsForReview] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState({ type: null, claimId: null });

    const fetchAdminReviewClaims = useCallback(async () => {
        console.log("[AdminReviewScreen] Fetching claims for admin review...");
        setIsLoading(true);
        try {
            // TODO: This endpoint needs to be secured for ADMINS on the backend
            const response = await fetch(`${API_BASE_URL}/api/claims/admin-review`, {
                // headers: { 'Authorization': `Bearer YOUR_ADMIN_TOKEN` } // Add if auth is implemented
            });
            if (!response.ok) {
                let errorMsg = "Failed to fetch claims for admin review.";
                try {
                    const errData = await response.json();
                    errorMsg = errData.message || errData.error || `Failed to fetch claims (status: ${response.status})`;
                } catch (e) {
                    // If parsing error message fails, use the status text or a generic message
                    errorMsg = response.statusText || `Server error: ${response.status}`;
                }
                throw new Error(errorMsg);
            }
            const data = await response.json();
            setClaimsForReview(Array.isArray(data) ? data : []);
            console.log(`[AdminReviewScreen] Fetched ${data.length} claims for review.`);
        } catch (error) {
            console.error("Error fetching admin review claims:", error);
            Alert.alert("Error Loading Claims", error.message || "Could not load claims for review. Please ensure the backend is running and accessible.");
            setClaimsForReview([]);
        } finally {
            setIsLoading(false);
        }
    }, []); // Empty dependency array as it doesn't depend on component state/props directly

    // Corrected useFocusEffect pattern
    useFocusEffect(
        useCallback(() => {
            let isActive = true;

            async function fetchData() {
                await fetchAdminReviewClaims();
            }

            fetchData();

            return () => {
                isActive = false; // Not strictly needed here as fetchAdminReviewClaims handles its own state
            };
        }, [fetchAdminReviewClaims]) // fetchAdminReviewClaims is memoized
    );

    const handleAdminAction = async (claimId, actionType) => {
        if (!currentUser || currentUser.role !== 'admin' || !currentUser.id) {
            Alert.alert("Unauthorized", "Only admins can perform this action. Ensure you are logged in as an admin.");
            return;
        }
        const endpointAction = actionType === 'approve' ? 'approve' : 'reject';
        setActionLoading({ type: actionType, claimId });

        let requestBody = { userId: currentUser.id }; // Admin's ID performing the action

        if (actionType === 'reject') {
            // Prompt for rejection reason
            Alert.prompt("Reject Claim", "Admin: Optional reason for rejection.",
                [
                    { text: "Cancel", style: "cancel", onPress: () => setActionLoading({ type: null, claimId: null }) },
                    {
                        text: "Confirm Rejection",
                        onPress: async (reason) => {
                            requestBody.rejectionReason = reason || "Rejected by admin.";
                            await performClaimAction(claimId, endpointAction, requestBody);
                        }
                    }
                ],
                "plain-text"
            );
        } else { // For approval
            await performClaimAction(claimId, endpointAction, requestBody);
        }
    };

    const performClaimAction = async (claimId, endpointAction, requestBody) => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/claims/${claimId}/${endpointAction}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' /*, 'Authorization': `Bearer YOUR_ADMIN_TOKEN` */ },
                body: JSON.stringify(requestBody)
            });
            const responseData = await response.json().catch(() => null);
            if (response.ok) {
                Alert.alert(`Claim ${endpointAction === 'approve' ? 'Approved' : 'Rejected'}`, responseData?.message || `Claim ${claimId} has been ${endpointAction === 'approve' ? 'approved' : 'rejected'}.`);
                fetchAdminReviewClaims(); // Refresh list
            } else {
                throw new Error(responseData?.error || responseData?.message || `Failed to ${endpointAction} claim.`);
            }
        } catch (error) {
            Alert.alert(`Error ${endpointAction === 'approve' ? 'Approving' : 'Rejecting'} Claim`, error.message);
        } finally {
            setActionLoading({ type: null, claimId: null });
        }
    };

    const renderClaimItem = ({ item: claim }) => (
        <Card style={styles.claimCard}>
            <Card.Content>
                <Title style={styles.cardTitle}><Text>Claim for Item ID: {claim.itemId}</Text></Title>
                <Paragraph><Text>Claimant: {claim.username || 'N/A'} (User ID: {claim.userId})</Text></Paragraph>
                <Paragraph style={styles.dateText}><Text>Date: {new Date(claim.claimDate).toLocaleString()}</Text></Paragraph>
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
                    style={[styles.statusChip, {backgroundColor: claim.status === 'FORWARDED_TO_ADMIN' ? colors.tertiaryContainer : colors.surfaceVariant }]} 
                    textStyle={{color: claim.status === 'FORWARDED_TO_ADMIN' ? colors.onTertiaryContainer : colors.onSurfaceVariant}}
                >
                    <Text>Status: {claim.status}</Text>
                </Chip>
            </Card.Content>
            {/* Actions only if the claim is in a state an admin can act upon, e.g., FORWARDED_TO_ADMIN */}
            {claim.status === 'FORWARDED_TO_ADMIN' && (
                <Card.Actions style={styles.actions}>
                    <Button 
                        mode="outlined" 
                        onPress={() => handleAdminAction(claim.id, 'reject')} 
                        textColor={colors.error} 
                        style={[styles.actionButton, {borderColor: colors.error}]}
                        loading={actionLoading.type === 'reject' && actionLoading.claimId === claim.id}
                        disabled={actionLoading.claimId !== null}
                        icon="close-circle-outline"
                    ><Text>Reject</Text></Button>
                    <Button 
                        mode="contained" 
                        onPress={() => handleAdminAction(claim.id, 'approve')} 
                        style={[styles.actionButton, {backgroundColor: colors.primary}]}
                        loading={actionLoading.type === 'approve' && actionLoading.claimId === claim.id}
                        disabled={actionLoading.claimId !== null}
                        icon="check-circle-outline"
                    ><Text>Approve</Text></Button>
                </Card.Actions>
            )}
        </Card>
    );

    if (isLoading && claimsForReview.length === 0) {
        return <View style={styles.loaderContainer}><PaperActivityIndicator size="large" animating={true} /><Text style={{marginTop:10, color: colors.onSurfaceVariant}}>Loading claims...</Text></View>;
    }

    return (
        <FlatList
            data={claimsForReview}
            renderItem={renderClaimItem}
            keyExtractor={(item) => item.id.toString()}
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

const styles = StyleSheet.create({
    container: { 
        flex: 1, 
        backgroundColor: '#f4f6f8', 
    },
    listContentContainer: { 
        padding: 10, 
    },
    loaderContainer: { 
        flex: 1, 
        justifyContent: 'center', 
        alignItems: 'center' 
    },
    claimCard: { 
        marginVertical: 8, 
        marginHorizontal:5, 
        elevation: 3, 
        borderRadius: 10 
    },
    cardTitle: {
        fontSize: 17,
        fontWeight: 'bold',
        marginBottom: 5,
    },
    dateText: {
        fontSize: 12,
        color: '#6c757d',
        marginBottom: 10,
    },
    divider: {
        marginVertical: 8,
    },
    detailLabel: {
        fontSize: 13,
        fontWeight: 'bold',
        color: '#495057',
        marginTop: 8,
    },
    descriptionText: { 
        marginVertical: 5, 
        fontSize: 14,
        lineHeight: 20,
    },
    proofImageContainer: {
        marginTop: 10,
    },
    proofImageLabel: { 
        fontWeight: 'bold', 
        marginBottom: 5,
        fontSize: 13,
        color: '#495057',
    },
    proofImage: { 
        width: '100%', 
        height: 200, 
        resizeMode: 'contain', 
        marginTop: 5, 
        borderRadius: 6, 
        backgroundColor: '#f0f0f0',
        borderWidth: 1,
        borderColor: '#e0e0e0',
    },
    statusChip: { 
        alignSelf: 'flex-start', 
        marginTop: 12, 
        paddingHorizontal: 8,
    },
    actions: { 
        justifyContent: 'space-between', 
        paddingTop: 12, 
        paddingBottom: 8,
        paddingHorizontal: 8,
        borderTopWidth: 1, 
        borderTopColor: '#f0f0f0' 
    },
    actionButton: {
        flex: 1,
        marginHorizontal: 4,
        borderRadius: 20, // Pill shape
    },
    emptyContainer: { 
        flexGrow: 1, 
        justifyContent: 'center', 
        alignItems: 'center', 
        padding: 20 
    },
    emptyContainerContent: { 
        alignItems: 'center'
    },
    emptyText: { 
        marginTop: 16, 
        fontSize: 16, 
        color: '#6c757d', 
        textAlign: 'center' 
    },
});
