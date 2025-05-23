import { Picker } from '@react-native-picker/picker';
import * as ImagePicker from 'expo-image-picker';
import { useEffect, useState } from 'react';
import { Alert, Image, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, IconButton, ActivityIndicator as PaperActivityIndicator, SegmentedButtons, Text, TextInput, useTheme } from 'react-native-paper';

const API_BASE_URL = 'http://10.7.41.239:8080';

export default function SubmitItemScreen({ navigation, route }) {
  const initialType = route.params?.type || 'lost';
  const receivedUserId = route.params?.userId;
  const { colors } = useTheme();

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'Other',
    location: '',
    type: initialType,
  });

  const [image, setImage] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const categories = ["ID Card", "Bag", "Laptop", "Keys", "Wallet", "Phone", "Electronics", "Clothing", "Book", "Other"];

  useEffect(() => {
    (async () => {
      if (Platform.OS !== 'web') {
        const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (status !== 'granted') {
          Alert.alert('Permission Required', 'Sorry, we need camera roll permissions to make this work!');
        }
      }
    })();
  }, []);

  const pickImage = async () => {
    console.log("[SubmitItemScreen] pickImage called");
    if (Platform.OS !== 'web') {
        const permStatus = await ImagePicker.getMediaLibraryPermissionsAsync();
        if (!permStatus.granted) {
            const { status: newStatus } = await ImagePicker.requestMediaLibraryPermissionsAsync();
            if (newStatus !== 'granted') {
                Alert.alert('Permission Required', 'Camera roll permission is needed to select an image.');
                return;
            }
        }
    }

    let mediaTypesToUse;
    if (ImagePicker.MediaType && ImagePicker.MediaType.Images) {
        console.log("[SubmitItemScreen] Using ImagePicker.MediaType.Images");
        mediaTypesToUse = ImagePicker.MediaType.Images;
    } else if (ImagePicker.MediaTypeOptions && ImagePicker.MediaTypeOptions.Images) {
        console.warn("[SubmitItemScreen] Fallback: Using ImagePicker.MediaTypeOptions.Images");
        mediaTypesToUse = ImagePicker.MediaTypeOptions.Images;
    } else {
        console.error("CRITICAL (SubmitItemScreen): Neither ImagePicker.MediaType.Images nor ImagePicker.MediaTypeOptions.Images is defined. Check expo-image-picker installation/version.");
        Alert.alert("Image Picker Unavailable", "The image picker module is not working correctly. Please check installation or restart the app.");
        return;
    }

    try {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: mediaTypesToUse,
            allowsEditing: true,
            aspect: [4, 3],
            quality: 0.7,
        });
        console.log("[SubmitItemScreen] ImagePicker result:", result);
        if (!result.canceled && result.assets && result.assets.length > 0) {
            setImage(result.assets[0]);
            console.log("[SubmitItemScreen] Image selected:", result.assets[0].uri);
        } else {
            console.log("[SubmitItemScreen] Image picking cancelled or no assets.");
        }
    } catch (error) {
        console.error("Error picking image in SubmitItemScreen:", error);
        Alert.alert("Image Picker Error", "Could not open image library: " + error.message);
    }
  };

  const handleSubmit = async () => {
    if (!formData.title.trim() || !formData.location.trim() || !formData.category) {
      Alert.alert('Missing Information', 'Please fill in Title, Location, and Category.');
      return;
    }
    if (!receivedUserId) {
      Alert.alert('Authentication Error', 'User information is missing. Please try logging in again.');
      return;
    }
    setIsSubmitting(true);
    const submitData = new FormData();
    submitData.append('item', JSON.stringify({
      title: formData.title.trim(),
      description: formData.description.trim(),
      category: formData.category,
      location: formData.location.trim(),
      isLost: formData.type === 'lost',
      userId: receivedUserId,
    }));
    if (image && image.uri) {
      const uriParts = image.uri.split('.');
      const fileType = uriParts[uriParts.length - 1];
      submitData.append('image', {
        uri: image.uri,
        name: `item-image-${Date.now()}.${fileType}`,
        type: `image/${fileType}`,
      });
    }
    try {
      const response = await fetch(`${API_BASE_URL}/api/items`, {
        method: 'POST',
        headers: { 'Accept': 'application/json' },
        body: submitData,
      });
      const responseText = await response.text();
      let responseData;
      try { responseData = JSON.parse(responseText); } catch (e) {
        if (!response.ok) throw new Error(responseText || `Failed to submit item. Server returned status ${response.status}.`);
        responseData = { message: responseText || "Item submitted (server response was not standard JSON)." };
      }
      if (response.ok) {
        Alert.alert('Success', responseData.message || 'Item submitted successfully!', [{ text: 'OK', onPress: () => navigation.goBack() }]);
      } else { throw new Error(responseData.error || responseData.message || 'Failed to submit item.'); }
    } catch (error) {
      console.error("SubmitItemScreen handleSubmit Error:", error);
      Alert.alert('Submission Error', error.message || 'An unexpected error occurred.');
    } finally {
      setIsSubmitting(false);
    }
  };
  
  return (
    <ScrollView style={styles.scrollContainer} contentContainerStyle={styles.container}>
      <Text style={[styles.headerTitle, {color: colors.primary}]}>Report {formData.type === 'lost' ? 'Lost' : 'Found'} Item</Text>
      <SegmentedButtons
        value={formData.type}
        onValueChange={(value) => setFormData({ ...formData, type: value })}
        buttons={[
          { value: 'lost', label: 'I Lost Something', icon: 'alert-decagram-outline', style: formData.type === 'lost' ? {backgroundColor: colors.errorContainer} : {}, labelStyle: formData.type === 'lost' ? {color: colors.onErrorContainer}: {} },
          { value: 'found', label: 'I Found Something', icon: 'hand-heart-outline', style: formData.type === 'found' ? {backgroundColor: colors.primaryContainer} : {}, labelStyle: formData.type === 'found' ? {color: colors.onPrimaryContainer}: {} },
        ]}
        style={styles.segmentedButton}
      />
      <TextInput
        label="Item Title*"
        value={formData.title}
        onChangeText={(text) => setFormData({ ...formData, title: text })}
        mode="outlined" style={styles.input} theme={{ roundness: 8 }}
        left={<TextInput.Icon icon="format-title" />}
      />
      <TextInput
        label="Description (include distinguishing features)"
        value={formData.description}
        onChangeText={(text) => setFormData({ ...formData, description: text })}
        mode="outlined" multiline numberOfLines={4} style={[styles.input, styles.multilineInput]} theme={{ roundness: 8 }}
        left={<TextInput.Icon icon="text-long" />}
      />
      <TextInput
        label="Location Where Item Was Lost/Found*"
        value={formData.location}
        onChangeText={(text) => setFormData({ ...formData, location: text })}
        mode="outlined" style={styles.input} theme={{ roundness: 8 }}
        left={<TextInput.Icon icon="map-marker-outline" />}
      />
      <Text style={[styles.label, {color: colors.onSurfaceVariant}]}>Category*:</Text>
      <View style={[styles.pickerContainer, {borderColor: colors.outline, borderRadius: 8}]}>
        <Picker
          selectedValue={formData.category}
          onValueChange={(itemValue) => setFormData({...formData, category: itemValue})}
          style={styles.picker}
          dropdownIconColor={colors.onSurfaceVariant}
        >
          {categories.map(cat => <Picker.Item key={cat} label={cat} value={cat} />)}
        </Picker>
      </View>
      <Button
        icon={image ? "image-edit-outline" : "camera-plus-outline"}
        mode="outlined" onPress={pickImage} style={styles.imagePickerButton} theme={{ roundness: 8 }}
        textColor={colors.primary}
      ><Text>{image ? 'Change Image' : 'Add Image (Optional)'}</Text></Button>
      {image && (
        <View style={styles.imagePreviewContainer}>
            <Image source={{ uri: image.uri }} style={styles.imagePreview} />
            <IconButton
                icon="close-circle" size={28} onPress={() => setImage(null)}
                style={[styles.removeImageButton, {backgroundColor: colors.surface}]}
                iconColor={colors.error}
            />
        </View>
      )}
      <Button
        mode="contained" onPress={handleSubmit} loading={isSubmitting} disabled={isSubmitting}
        style={styles.submitButton} labelStyle={styles.submitButtonLabel}
        icon={isSubmitting ? () => <PaperActivityIndicator animating={true} size="small" color={colors.onPrimary}/> : "send-outline"}
        theme={{ roundness: 8 }}
        contentStyle={{paddingVertical: 4}}
      ><Text>{isSubmitting ? 'SUBMITTING...' : 'SUBMIT ITEM'}</Text></Button>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContainer: { flex: 1, backgroundColor: '#f8f9fa', },
  container: { padding: 20, paddingBottom: 40, },
  headerTitle: { fontSize: 22, fontWeight: 'bold', textAlign: 'center', marginBottom: 20, },
  segmentedButton: { marginBottom: 20, },
  input: { marginBottom: 16, backgroundColor: '#fff', },
  multilineInput: { minHeight: 100, textAlignVertical: 'top', },
  label: { fontSize: 14, marginBottom: 8, fontWeight: '500', },
  pickerContainer: { borderWidth: 1, borderRadius: 8, marginBottom: 16, backgroundColor: '#fff', },
  picker: { height: 50, width: '100%', },
  imagePickerButton: { marginVertical: 10, paddingVertical: 6, borderColor: '#6200ee', },
  imagePreviewContainer: { alignItems: 'center', marginVertical: 15, position: 'relative', backgroundColor: '#e0e0e0', borderRadius: 8, padding: 5, },
  imagePreview: { width: '100%', height: 200, resizeMode: 'contain', borderRadius: 6, },
  removeImageButton: { position: 'absolute', top: -12, right: -12, borderRadius: 16, elevation: 2, },
  submitButton: { marginTop: 20, elevation: 2, },
  submitButtonLabel: { fontWeight: 'bold', fontSize: 16, },
});
