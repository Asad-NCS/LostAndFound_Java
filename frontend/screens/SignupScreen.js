import { useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  View
} from 'react-native';
import {
  Button,
  ActivityIndicator as PaperActivityIndicator,
  SegmentedButtons,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const API_BASE_URL = 'http://10.7.41.239:8080'; // Ensure this is your correct IP

export default function SignupScreen({ navigation }) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('user');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { colors } = useTheme();

  const handleSignup = async () => {
    if (!email.trim() || !password || !confirmPassword || !username.trim()) {
      Alert.alert('Validation Error', 'Please fill in all fields.');
      return;
    }
    if (password !== confirmPassword) {
      Alert.alert('Password Mismatch', 'Passwords do not match.');
      return;
    }
    if (password.length < 6) {
        Alert.alert('Weak Password', 'Password should be at least 6 characters long.');
        return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: username.trim(),
          email: email.trim().toLowerCase(),
          password: password,
          role: role,
        }),
      });

      const responseText = await response.text();
      let responseData;
      try {
        responseData = JSON.parse(responseText);
      } catch (e) {
        console.error("Signup Error: Failed to parse server response:", responseText);
        if (!response.ok) throw new Error(responseText || "Signup failed. Server returned an unreadable response.");
        responseData = { message: responseText || "Registration successful (server response was not standard JSON)." };
      }

      if (!response.ok) {
        throw new Error(responseData.error || responseData.message || 'Signup failed. Please try again.');
      }

      Alert.alert(
        'Registration Successful!',
        responseData.message || 'Your account has been created. Please log in.',
        [{ text: 'OK', onPress: () => navigation.navigate('Login') }]
      );
    } catch (err) {
      console.error("Signup Catch Error:", err);
      Alert.alert('Signup Error', err.message || 'Something went wrong during registration.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.scrollContainer}>
        <View style={styles.container}>
            <Icon name="account-plus-outline" size={60} color={colors.primary} style={styles.headerIcon} />
            <Text style={styles.title}>Create New Account</Text>

            <TextInput
                label="Username"
                value={username}
                onChangeText={setUsername}
                mode="outlined"
                style={styles.input}
                left={<TextInput.Icon icon="account-outline" />}
                theme={{ roundness: 8 }}
            />
            <TextInput
                label="Email"
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                mode="outlined"
                style={styles.input}
                left={<TextInput.Icon icon="email-outline" />}
                theme={{ roundness: 8 }}
            />
            <TextInput
                label="Password"
                value={password}
                onChangeText={setPassword}
                secureTextEntry={!showPassword}
                mode="outlined"
                style={styles.input}
                left={<TextInput.Icon icon="lock-outline" />}
                right={<TextInput.Icon icon={showPassword ? "eye-off-outline" : "eye-outline"} onPress={() => setShowPassword(!showPassword)} />}
                theme={{ roundness: 8 }}
            />
            <TextInput
                label="Confirm Password"
                value={confirmPassword}
                onChangeText={setConfirmPassword}
                secureTextEntry={!showConfirmPassword}
                mode="outlined"
                style={styles.input}
                left={<TextInput.Icon icon="lock-check-outline" />}
                right={<TextInput.Icon icon={showConfirmPassword ? "eye-off-outline" : "eye-outline"} onPress={() => setShowConfirmPassword(!showConfirmPassword)} />}
                theme={{ roundness: 8 }}
            />

            <Text style={[styles.label, { color: colors.onSurfaceVariant }]}>Select Role:</Text>
            <SegmentedButtons
                value={role}
                onValueChange={setRole}
                style={styles.segmentedButton}
                buttons={[
                    { value: 'user', label: 'User', icon: 'account-outline' },
                    { value: 'admin', label: 'Admin', icon: 'shield-account-outline' },
                ]}
            />

            <Button
                mode="contained"
                onPress={handleSignup}
                loading={isLoading}
                disabled={isLoading}
                style={styles.button}
                labelStyle={styles.buttonLabel}
                icon={isLoading ? () => <PaperActivityIndicator animating={true} size="small" color={colors.onPrimary}/> : "account-plus"}
                theme={{ roundness: 8 }}
            >
                {/* Ensure Button children are Text components or valid React Native Paper Button content */}
                <Text>{isLoading ? 'CREATING ACCOUNT...' : 'SIGN UP'}</Text>
            </Button>
            <TouchableOpacity onPress={() => navigation.navigate('Login')} style={styles.loginLink}>
                <Text style={[styles.footerText, {color: colors.onBackground}]}>Already have an account? </Text>
                <Text style={[styles.linkButton, { color: colors.primary }]}>Log In</Text>
            </TouchableOpacity>
        </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContainer: {
    flexGrow: 1,
    justifyContent: 'center',
    backgroundColor: '#f8f9fa',
  },
  container: { 
    paddingHorizontal: 24, 
    paddingVertical: 20,
  },
  headerIcon: {
    alignSelf: 'center',
    marginBottom: 20,
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 25,
    color: '#343a40',
  },
  input: { 
    marginBottom: 16,
    backgroundColor: '#fff',
   },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    marginLeft: 4,
  },
  segmentedButton: {
    marginBottom: 20,
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
  loginLink: {
    marginTop: 25,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  footerText: {
    fontSize: 14,
  },
  linkButton: {
    fontWeight: 'bold',
    fontSize: 14,
  },
});
