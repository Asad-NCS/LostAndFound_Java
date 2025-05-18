import { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import { Button, Text, TextInput, useTheme } from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const API_BASE_URL = 'http://10.7.41.147:8080';

export default function VerifyScreen({ route, navigation }) {
  // Assuming email is passed if verification is email-based
  // If it's claim-based verification, you might receive claimId or itemId
  const { email, claimId, itemId } = route.params || {}; 
  const [otp, setOtp] = useState('');
  const [isLoading, setIsLoading] = useState(false); // Renamed for consistency
  const { colors } = useTheme();

  const handleVerifyOtp = async () => {
    if (!otp.trim()) {
      Alert.alert('Input Error', 'Please enter the verification code.');
      return;
    }
    if (otp.length < 4 || otp.length > 6) { // Common OTP length
        Alert.alert('Invalid Code', 'Verification code is usually 4 to 6 digits.');
        return;
    }
    
    setIsLoading(true);
    try {
      // Adjust endpoint and body based on what you are verifying (account or claim)
      let endpoint = '';
      let body = {};

      if (email) { // Account verification after signup
        endpoint = `${API_BASE_URL}/api/auth/verify`; // Your existing endpoint
        body = { email, code: otp.trim() };
      } else if (claimId && itemId) { // Claim verification (this logic needs to be well-defined)
        // This is a placeholder for claim verification, the actual API might differ
        endpoint = `${API_BASE_URL}/api/claims/verify-ownership`; // Example endpoint
        body = { claimId, itemId, verificationCode: otp.trim() };
      } else {
        throw new Error("Verification context is unclear (no email or claimId).");
      }
      
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      const responseData = await response.json();
      if (!response.ok) {
        throw new Error(responseData.message || 'Verification failed. Please check the code and try again.');
      }

      Alert.alert('Success!', responseData.message || 'Verification successful.', [
        { text: 'OK', onPress: () => navigation.navigate(email ? 'Login' : 'Home') } // Navigate appropriately
      ]);

    } catch (err) {
      Alert.alert('Verification Error', err.message || 'Something went wrong during verification.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Icon name="shield-check-outline" size={60} color={colors.primary} style={styles.headerIcon} />
      <Text style={styles.title}>Verify Your Account</Text>
      <Text style={styles.subtitle}>
        {email ? `A verification code was sent to ${email}.` : 'Enter the verification code provided.'}
      </Text>
      
      <TextInput
        label="Verification Code"
        value={otp}
        onChangeText={setOtp}
        keyboardType="number-pad"
        mode="outlined"
        style={styles.input}
        maxLength={6} // Common OTP length
        left={<TextInput.Icon icon="numeric" />}
        theme={{ roundness: 8 }}
      />
      <Button
        mode="contained"
        onPress={handleVerifyOtp}
        loading={isLoading}
        disabled={isLoading}
        style={styles.button}
        labelStyle={styles.buttonLabel}
        icon={isLoading ? null : "check-circle-outline"}
        theme={{ roundness: 8 }}
      >
        {isLoading ? 'VERIFYING...' : 'VERIFY'}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingVertical: 20,
    justifyContent: 'center',
    backgroundColor: '#f8f9fa',
  },
  headerIcon: {
    alignSelf: 'center',
    marginBottom: 20,
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 10,
    color: '#343a40',
  },
  subtitle: {
    fontSize: 15,
    textAlign: 'center',
    color: '#6c757d',
    marginBottom: 30,
    lineHeight: 22,
  },
  input: {
    marginBottom: 16,
    backgroundColor: '#fff',
  },
  button: {
    marginTop: 12,
    paddingVertical: 10,
    elevation: 2,
  },
  buttonLabel: {
      fontWeight: 'bold',
      fontSize: 16,
  },
});