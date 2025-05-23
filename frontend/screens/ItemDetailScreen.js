import { useFocusEffect } from '@react-navigation/native';
import * as ImagePicker from 'expo-image-picker';
import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Image,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  Button,
  Card,
  Chip,
  Divider,
  IconButton,
  ActivityIndicator as PaperActivityIndicator,
  Paragraph,
  Text,
  TextInput,
  Title,
  useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

// Ensure this IP is correct and your backend is running there
const API_BASE_URL = 'http://10.7.41.239:8080'; // Replace with your actual backend IP if different

export default function ItemDetailScreen({ route, navigation }) {
  const { item: initialItem, currentUser } = route.params;

  const [item, setItem] = useState(initialItem);
  const { colors } = useTheme();

  const [showClaimModal, setShowClaimModal] = useState(false);
  const [claimDescription, setClaimDescription] = useState('');
  const [proofImage, setProofImage] = useState(null);
  const [isSubmittingClaim, setIsSubmittingClaim] = useState(false);

  const [itemClaims, setItemClaims] = useState([]);
  const [isLoadingClaims, setIsLoadingClaims] = useState(false);
  const [actionLoading, setActionLoading] = useState({ type: null, claimId: null });

  const isCurrentUserItemReporter = currentUser && item && item.user && currentUser.id === item.user.id;
  const canMakeClaim = item && !item.isLost && !isCurrentUserItemReporter && !item.claimed;
  const showManageClaimsSection = isCurrentUserItemReporter && item && !item.isLost && !item.claimed;

  useEffect(() => {
    console.log('[ItemDetailScreen] DEBUG State Update:');
    console.log('  currentUser.id:', currentUser?.id);
    console.log('  item.id:', item?.id, 'item.user.id:', item?.user?.id, 'item.isLost:', item?.isLost, 'item.claimed:', item?.claimed);
    console.log('  isCurrentUserItemReporter:', isCurrentUserItemReporter);
    console.log('  showManageClaimsSection:', showManageClaimsSection);
  }, [currentUser, item, isCurrentUserItemReporter, showManageClaimsSection]);

  useFocusEffect(
    useCallback(() => {
      let isActive = true;
      async function fetchItemDataOnFocus() {
        if (!initialItem?.id) return;
        console.log(`[ItemDetailScreen] useFocusEffect: Fetching item ID: ${initialItem.id}`);
        try {
          const itemRes = await fetch(`${API_BASE_URL}/api/items/${initialItem.id}`);
          if (!isActive) return;
          if (itemRes.ok) {
            const updatedItemData = await itemRes.json();
            if (isActive) setItem(updatedItemData);
          } else {
            console.warn(`[ItemDetailScreen] useFocusEffect: Failed to fetch item, status: ${itemRes.status}`);
          }
        } catch (e) {
          console.error("[ItemDetailScreen] useFocusEffect: Error fetching item:", e);
        }
      }
      fetchItemDataOnFocus();
      return () => { isActive = false; };
    }, [initialItem?.id])
  );
  
  const fetchClaimsForCurrentItem = useCallback(async () => {
    const currentItemIsReporters = currentUser && item && item.user && currentUser.id === item.user.id;
    const shouldFetch = currentItemIsReporters && item && !item.isLost && item.id && !item.claimed;
    if (!shouldFetch) { setItemClaims([]); return; }
    if (isLoadingClaims) return; 
    
    console.log(`[ItemDetailScreen] fetchClaimsForCurrentItem: Fetching for item ${item.id}`);
    setIsLoadingClaims(true);
    try {
      const claimsResponse = await fetch(`${API_BASE_URL}/api/claims/item/${item.id}`);
      if (!claimsResponse.ok) {
        const errData = await claimsResponse.json().catch(() => ({ message: "Error fetching claims" }));
        throw new Error(errData.message);
      }
      const claimsData = await claimsResponse.json();
      setItemClaims(Array.isArray(claimsData) ? claimsData : []);
    } catch (error) {
      console.error("[ItemDetailScreen] fetchClaimsForCurrentItem: Error:", error);
      setItemClaims([]);
    } finally {
      setIsLoadingClaims(false);
    }
  }, [item, currentUser]);

  useEffect(() => {
    if (item && item.id && currentUser && currentUser.id) {
        fetchClaimsForCurrentItem();
    }
  }, [item, currentUser, fetchClaimsForCurrentItem]);

  useEffect(() => {
    setItem(initialItem);
  }, [initialItem]);

  // Updated pickProofImage function with fallback logic
  const pickProofImage = async () => { 
    console.log("[ItemDetailScreen] pickProofImage called");
    if (Platform.OS !== 'web') {
      const permStatus = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (permStatus.status !== 'granted') { 
        Alert.alert('Permission Required', 'Camera roll permission is needed to select a proof image.'); 
        return; 
      }
    }

    let mediaTypesToUse;
    if (ImagePicker.MediaType && ImagePicker.MediaType.Images) {
        console.log("[ItemDetailScreen] Using ImagePicker.MediaType.Images");
        mediaTypesToUse = ImagePicker.MediaType.Images;
    } else if (ImagePicker.MediaTypeOptions && ImagePicker.MediaTypeOptions.Images) {
        console.warn("[ItemDetailScreen] Fallback: Using ImagePicker.MediaTypeOptions.Images");
        mediaTypesToUse = ImagePicker.MediaTypeOptions.Images; // Fallback to older API
    } else {
        console.error("CRITICAL (ItemDetailScreen): Neither ImagePicker.MediaType.Images nor ImagePicker.MediaTypeOptions.Images is defined. Check expo-image-picker installation/version.");
        Alert.alert("Image Picker Unavailable", "The image picker module is not working correctly. Please check installation or restart the app.");
        return; // Exit if no valid media type option is found
    }

    try {
      let result = await ImagePicker.launchImageLibraryAsync({ 
        mediaTypes: mediaTypesToUse, 
        allowsEditing: true, 
        aspect: [4, 3], 
        quality: 0.7 
      });
      console.log("[ItemDetailScreen] ImagePicker result:", result);
      if (!result.canceled && result.assets && result.assets.length > 0) {
        setProofImage(result.assets[0]);
        console.log("[ItemDetailScreen] Proof image selected:", result.assets[0].uri);
      } else {
        console.log("[ItemDetailScreen] Proof image picking cancelled or no assets.");
      }
    } catch (e) { 
      console.error("ImagePicker Error during launchImageLibraryAsync (ItemDetailScreen):", e); 
      Alert.alert("Image Picker Error", "Could not open image library. " + e.message); 
    }
  };

  const handleSubmitClaim = async () => { 
    if (!claimDescription.trim()) { Alert.alert('Description Required', 'Please describe why this item belongs to you.'); return; }
    if (!currentUser || !currentUser.id) { Alert.alert('Authentication Error', 'User information not found. Please log in again.'); return; }
    if (!item || !item.id) { Alert.alert('Error', 'Item information is missing. Cannot submit claim.'); return; }
    setIsSubmittingClaim(true);
    const claimFormData = new FormData();
    const claimDataPayload = { itemId: item.id, userId: currentUser.id, description: claimDescription.trim() };
    claimFormData.append('claimData', JSON.stringify(claimDataPayload));
    if (proofImage && proofImage.uri) {
      const uriParts = proofImage.uri.split('.'); const fileType = uriParts[uriParts.length - 1];
      claimFormData.append('proofImage', { uri: proofImage.uri, name: `claim-proof-${Date.now()}.${fileType}`, type: `image/${fileType}`});
    }
    try {
      const response = await fetch(`${API_BASE_URL}/api/claims`, { method: 'POST', headers: { 'Accept': 'application/json' }, body: claimFormData });
      const responseText = await response.text(); 
      let responseData;
      try { responseData = JSON.parse(responseText); } catch (e) {
        if (!response.ok) throw new Error(responseText || `Failed to submit claim. Server returned status ${response.status}.`);
        responseData = { message: responseText || "Claim submitted (server response was not standard JSON)." };
      }
      if (response.ok) {
        Alert.alert('Claim Submitted', responseData.message || 'Your claim has been submitted successfully.');
        setShowClaimModal(false); setClaimDescription(''); setProofImage(null);
      } else { throw new Error(responseData.error || responseData.message || 'Failed to submit claim.'); }
    } catch (error) {
      Alert.alert('Claim Submission Error', error.message || 'An unexpected error occurred.');
    } finally {
      setIsSubmittingClaim(false);
    }
  };

  const handleForwardClaimToAdmin = async (claimIdToForward) => { 
    if (!currentUser || !currentUser.id) { Alert.alert("Authentication Error", "Current user ID is missing."); return; }
    setActionLoading({ type: 'forward', claimId: claimIdToForward });
    try {
      const response = await fetch(`${API_BASE_URL}/api/claims/${claimIdToForward}/forward-to-admin`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json', },
        body: JSON.stringify({ userId: currentUser.id }) 
      });
      const responseData = await response.json().catch(() => ({ message: "Claim forwarded, but server response was not standard JSON." }));
      if (response.ok) {
        Alert.alert("Claim Forwarded", responseData.message || `Claim ID ${claimIdToForward} forwarded to admin.`);
        fetchClaimsForCurrentItem(); 
      } else { throw new Error(responseData?.error || responseData?.message || "Failed to forward claim."); }
    } catch (error) { Alert.alert("Error Forwarding Claim", error.message); }
    finally { setActionLoading({ type: null, claimId: null }); }
  };

  const renderItemProperty = (label, value, iconName, isChip = false) => { 
    if (value == null && !isChip && typeof value !== 'boolean' && typeof value !== 'number') { return null; }
    if (typeof value === 'string' && value.trim() === "" && !isChip) { return null; }
    return ( <View style={styles.propertyContainer}> {iconName && <Icon name={iconName} size={20} color={colors.onSurfaceVariant} style={styles.propertyIcon} />} <Text style={styles.propertyLabel}>{label}:</Text> {isChip ? value : <Paragraph style={styles.propertyValue}>{String(value) !== '' ? String(value) : 'Not specified'}</Paragraph>} </View> );
  };

  if (!item || !item.id) { 
    return <View style={styles.fullScreenLoader}><PaperActivityIndicator size="large" /><Text style={{marginTop: 10, color: colors.onSurfaceVariant}}>Loading item details...</Text></View>; 
  }

  let imageUri = null;
  if (item.imageUrl) {
    if (item.imageUrl.startsWith('http')) imageUri = item.imageUrl;
    else if (item.imageUrl.startsWith('/api/items/images/')) imageUri = `${API_BASE_URL}${item.imageUrl}`;
    else imageUri = `${API_BASE_URL}/api/items/images/${item.imageUrl}`;
  }

  return (
    <ScrollView style={styles.screenContainer} contentContainerStyle={styles.scrollContentContainer}>
      <Card style={styles.card}>
        {imageUri ? ( <Image source={{ uri: imageUri }} style={styles.image} resizeMode="cover" onError={(e) => console.warn("Item image load error", e.nativeEvent.error)}/> ) : ( <View style={[styles.image, styles.imagePlaceholder]}><Icon name="camera-off-outline" size={60} color={colors.onSurfaceVariant} /><Text style={{color: colors.onSurfaceVariant, marginTop: 8}}>No Image Available</Text></View> )}
        <Card.Content style={styles.content}>
            <Title style={styles.title}>{item.title || 'Item Title Not Available'}</Title>
            <Paragraph style={styles.reporterInfo}>
                <Text>Reported as </Text><Text style={{fontWeight: 'bold'}}>{item.isLost ? 'LOST' : 'FOUND'}</Text><Text> by: {item.user?.username || 'Unknown User'}</Text>
            </Paragraph>
            <Divider style={styles.divider} />
            {renderItemProperty('Description', item.description, 'text-box-outline')}
            {renderItemProperty('Location Reported', item.location, 'map-marker-radius-outline')}
            {renderItemProperty('Category', item.category?.name || item.category, 'tag-outline')}
            {renderItemProperty('Status', <Chip icon={item.claimed ? "lock-check-outline" : (item.isLost ? "alert-decagram-outline" : "hand-heart-outline")} style={[styles.statusChip, {backgroundColor: item.claimed ? colors.surfaceDisabled : (item.isLost ? colors.errorContainer : colors.primaryContainer)}]} textStyle={{color: item.claimed ? colors.onSurfaceVariant : (item.isLost ? colors.onErrorContainer : colors.onPrimaryContainer)}}><Text>{item.claimed ? `Claimed by ${item.claimedByUser?.username || 'Verified Owner'}` : (item.isLost ? 'Currently Lost' : 'Found - Available to Claim')}</Text></Chip>, null, true )}
            <Divider style={styles.divider} />
            {canMakeClaim && ( <Button mode="contained" icon="gavel" onPress={() => setShowClaimModal(true)} style={styles.makeClaimButton} labelStyle={{fontSize: 16, fontWeight: 'bold'}} theme={{roundness: 8}}><Text>This is Mine! Make a Claim</Text></Button> )}
            {item.claimed && !isCurrentUserItemReporter && (!item.claimedByUser || !currentUser || item.claimedByUser.id !== currentUser.id) && ( <View style={styles.claimedMessageContainer}><Icon name="check-circle-outline" size={24} color={colors.primary} /><Text style={styles.claimedMessage}>This item has already been claimed.</Text></View> )}
            {item.claimed && item.claimedByUser && currentUser && item.claimedByUser.id === currentUser.id && ( <View style={styles.claimedMessageContainer}><Icon name="check-circle" size={24} color={colors.primary} /><Text style={styles.claimedMessage}>Your claim for this item was approved!</Text></View> )}
        </Card.Content>
      </Card>

      {showManageClaimsSection && ( 
        <Card style={styles.manageClaimsCard}>
          <Card.Title 
            title={`Claims Received (${itemClaims.filter(c => c.status === 'PENDING' || c.status === 'FORWARDED_TO_ADMIN').length} Active)`} 
            titleStyle={styles.sectionTitleInsideCard}
            subtitle="Review claims made on this item you found"
            subtitleStyle={{fontSize: 13}}
          />
          <Card.Content>
            {isLoadingClaims ? ( <PaperActivityIndicator animating={true} style={{marginVertical: 20}} /> ) : itemClaims.length === 0 ? ( <Text style={styles.noClaimsText}>No claims have been made for this item yet.</Text> ) : (
              itemClaims.map((claim) => (
                <Card key={claim.id} style={styles.claimReviewCard}>
                  <Card.Content>
                    <Title style={styles.claimantTitle}><Text>Claim by: {claim.username || 'N/A'}</Text></Title>
                    <Paragraph style={styles.claimDate}><Text>Date: {new Date(claim.claimDate).toLocaleDateString()}</Text></Paragraph>
                    <Divider style={styles.claimDivider} />
                    <Text style={styles.claimDetailLabel}>Claimant's Proof Description:</Text>
                    <Paragraph style={styles.claimDetailText}>{claim.description}</Paragraph>
                    {claim.proofImagePath && ( <>
                        <Text style={styles.claimDetailLabel}>Claimant's Proof Image:</Text>
                        <TouchableOpacity onPress={() => Alert.alert("Proof Image", "Implement full screen image view or zoom functionality here.")}>
                            <Image source={{ uri: `${API_BASE_URL}/api/items/images/${claim.proofImagePath}` }} style={styles.proofImage} resizeMode="contain" />
                        </TouchableOpacity>
                      </> )}
                  </Card.Content>
                  {claim.status === 'PENDING' && (
                    <Card.Actions style={styles.claimActions}>
                      <Button 
                        mode="contained" 
                        icon="arrow-right-bold-box-outline"
                        onPress={() => handleForwardClaimToAdmin(claim.id)} 
                        style={[styles.actionButton, {backgroundColor: colors.secondaryContainer, flex:1}]}
                        textColor={colors.onSecondaryContainer}
                        loading={actionLoading.type === 'forward' && actionLoading.claimId === claim.id}
                        disabled={actionLoading.claimId !== null}
                        compact
                      ><Text>Forward to Admin</Text></Button>
                    </Card.Actions>
                  )}
                  {claim.status === 'FORWARDED_TO_ADMIN' && <Chip icon="progress-clock" style={[styles.statusFeedbackChip, {backgroundColor: colors.tertiaryContainer}]} textStyle={{color: colors.onTertiaryContainer}}><Text>Awaiting Admin Review</Text></Chip>}
                  {claim.status === 'APPROVED' && <Chip icon="check-circle" style={[styles.statusFeedbackChip, {backgroundColor: colors.primaryContainer}]} textStyle={{color: colors.onPrimaryContainer}}><Text>Approved by Admin</Text></Chip>}
                  {claim.status === 'REJECTED' && <Chip icon="close-circle" style={[styles.statusFeedbackChip, {backgroundColor: colors.errorContainer}]} textStyle={{color: colors.onErrorContainer}}><Text>Rejected by Admin</Text></Chip>}
                </Card>
              ))
            )}
          </Card.Content>
        </Card>
      )}

      <Modal visible={showClaimModal} onDismiss={() => setShowClaimModal(false)} contentContainerStyle={[styles.modalContainer, {backgroundColor: colors.elevation.level2}]}>
        <ScrollView keyboardShouldPersistTaps="handled">
          <View style={styles.modalHeader}><Title style={styles.modalTitle}><Text>Submit Your Claim for "{item?.title || 'this item'}"</Text></Title><IconButton icon="close-circle-outline" size={28} onPress={() => setShowClaimModal(false)} style={styles.closeModalButton} iconColor={colors.onSurfaceVariant}/></View>
          <Paragraph style={styles.modalInstructions}><Text>To help the finder verify your ownership, please provide a detailed description and, if possible, a photo as proof (e.g., you with the item, a receipt, unique marks).</Text></Paragraph>
          <TextInput label="Proof Description*" value={claimDescription} onChangeText={setClaimDescription} mode="outlined" multiline numberOfLines={5} style={styles.modalInput} theme={{ roundness: 8 }} />
          <Button icon={proofImage ? "image-multiple-outline" : "camera-plus-outline"} mode="outlined" onPress={pickProofImage} style={styles.modalImagePickerButton} theme={{ roundness: 8 }} textColor={colors.primary}><Text>{proofImage ? 'Change Proof Image' : 'Add Proof Image (Optional)'}</Text></Button>
          {proofImage && (<View style={styles.modalImagePreviewContainer}><Image source={{ uri: proofImage.uri }} style={styles.modalImagePreview} /><IconButton icon="alpha-x-circle" size={30} onPress={() => setProofImage(null)} style={styles.modalRemoveImageButton} iconColor={colors.error} containerColor={colors.surface} /></View>)}
          <Button mode="contained" onPress={handleSubmitClaim} loading={isSubmittingClaim} disabled={isSubmittingClaim} style={styles.modalSubmitButton} labelStyle={{fontSize: 16, fontWeight: 'bold'}} icon="send-check-outline" theme={{ roundness: 8 }} contentStyle={{paddingVertical: 6}}><Text>{isSubmittingClaim ? 'SUBMITTING...' : 'SUBMIT CLAIM'}</Text></Button>
        </ScrollView>
      </Modal>
    </ScrollView>
  );
}

// --- Styles ---
// (Same styles as item_detail_screen_final_v8)
const styles = StyleSheet.create({
  screenContainer: { flex: 1, backgroundColor: '#f4f6f8' },
  scrollContentContainer: { paddingBottom: 30, },
  card: { marginHorizontal: 12, marginTop:12, marginBottom: 12, borderRadius: 12, elevation: 3, backgroundColor: '#ffffff', },
  image: { width: '100%', height: 280, borderTopLeftRadius: 12, borderTopRightRadius: 12, backgroundColor: '#e9ecef', },
  imagePlaceholder: { justifyContent: 'center', alignItems: 'center', },
  content: { padding: 16, },
  title: { fontSize: 24, fontWeight: '700', marginBottom: 6, color: '#212529', lineHeight: 30, },
  reporterInfo: { fontSize: 13, color: '#6c757d', marginBottom: 14, fontStyle: 'italic', },
  divider: { marginVertical: 14, height: 1, backgroundColor: '#e0e0e0', },
  propertyContainer: { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 12, paddingVertical: 4, },
  propertyIcon: { marginRight: 12, marginTop: 3, width: 20, textAlign: 'center', },
  propertyLabel: { fontSize: 14, fontWeight: '600', color: '#495057', marginRight: 8, width: 100, },
  propertyValue: { fontSize: 14, color: '#343a40', flexShrink: 1, lineHeight: 21, },
  statusChip: { height: 'auto', paddingVertical: 6, paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center', borderRadius: 18, alignSelf: 'flex-start', borderWidth: 1, borderColor: 'transparent', },
  makeClaimButton: { marginTop: 20, paddingVertical: 10, borderRadius: 8, elevation: 2, },
  claimedMessageContainer: { flexDirection: 'row', alignItems: 'center', padding: 14, backgroundColor: '#e3f2fd', borderRadius: 8, marginTop: 20, borderWidth: 1, borderColor: '#b3e0ff', },
  claimedMessage: { marginLeft: 10, fontSize: 15, color: '#0d47a1', flexShrink: 1, lineHeight: 22, },
  manageClaimsCard: { marginHorizontal:12, marginTop: 10, marginBottom: 20, elevation: 2, borderRadius: 12, backgroundColor: '#ffffff', },
  sectionTitleInsideCard: { fontSize: 18, fontWeight: '600', color: '#343a40', /* Card.Title handles padding */ },
  noClaimsText: { textAlign: 'center', paddingVertical: 25, color: '#6c757d', fontStyle: 'italic', fontSize: 15, },
  claimReviewCard: { marginBottom: 16, elevation: 1, borderColor: '#dee2e6', borderWidth: 1, borderRadius: 10, },
  claimantTitle: { fontSize: 16, fontWeight: 'bold', color: '#212529', },
  claimDate: { fontSize: 12, color: '#6c757d', marginBottom: 8, },
  claimDivider: { marginVertical: 10, },
  claimDetailLabel: { fontSize: 13, fontWeight: '600', color: '#495057', marginTop: 10, },
  claimDetailText: { fontSize: 14, color: '#343a40', marginBottom: 10, lineHeight: 20, marginLeft: 0, },
  proofImage: { width: '100%', height: 220, resizeMode: 'contain', borderRadius: 8, marginTop: 8, marginBottom:12, backgroundColor: '#f8f9fa', alignSelf: 'center', borderWidth: 1, borderColor: '#e0e0e0', },
  claimActions: { justifyContent: 'center', paddingTop: 12, paddingBottom:8, paddingHorizontal: 8, borderTopWidth:1, borderTopColor: '#f0f0f0', }, 
  actionButton: { marginHorizontal: 4, borderRadius: 20, }, 
  statusFeedbackChip: { marginVertical:10, alignSelf: 'center', paddingVertical: 6, paddingHorizontal: 12, borderRadius: 16, },
  modalContainer: { marginHorizontal: 15, marginVertical: 30, padding: 20, borderRadius: 16, maxHeight: '90%', elevation: 5, shadowColor: '#000', shadowOffset: { width: 0, height: 2, }, shadowOpacity: 0.25, shadowRadius: 3.84, },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 15, borderBottomWidth: 1, borderBottomColor: '#e0e0e0', paddingBottom: 10, },
  modalTitle: { fontSize: 18, fontWeight: '600', flex: 1, color: '#333', },
  closeModalButton: { margin: -10, },
  modalInstructions: { fontSize: 14, lineHeight: 21, marginBottom: 20, color: '#495057', },
  modalInput: { marginBottom: 16, minHeight: 120, textAlignVertical: 'top', backgroundColor: '#fff', },
  modalImagePickerButton: { marginVertical: 12, paddingVertical: 8, borderColor: '#6200ee', },
  modalImagePreviewContainer: { alignItems: 'center', marginVertical: 15, position: 'relative', backgroundColor: '#f0f0f0', borderRadius: 8, padding: 8, },
  modalImagePreview: { width: '100%', height: 180, resizeMode: 'contain', borderRadius: 6, },
  modalRemoveImageButton: { position: 'absolute', top: -14, right: -14, borderRadius: 18, elevation: 3, backgroundColor: 'white' },
  modalSubmitButton: { marginTop: 25, paddingVertical: 10, elevation: 2, },
  fullScreenLoader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f4f6f8', }
});
