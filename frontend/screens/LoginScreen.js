import { useState } from 'react'; // Ensure React is imported
import { Alert, Image, StyleSheet, TouchableOpacity, View } from 'react-native';
import { Button, ActivityIndicator as PaperActivityIndicator, Text, TextInput, useTheme } from 'react-native-paper';
import logoImage from '../assets/images/react-logo.png'; // Ensure this path is correct

const API_BASE_URL = 'http://10.7.41.239:8080'; // Or your current IP

const LoginScreen = ({ navigation, setUser }) => { // setUser is the updateUser function from App.js
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { colors } = useTheme();

  const handleLogin = async () => {
    if (!email.trim() || !password) {
      Alert.alert('Validation Error', 'Both email and password are required.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email.trim().toLowerCase(),
          password,
        }),
      });

      const responseText = await response.text();
      let data;
      try {
        data = JSON.parse(responseText);
      } catch (e) {
        console.error("Login Error: Failed to parse server response:", responseText);
        // If parsing fails but response indicates an error, use the text
        if (!response.ok) throw new Error(responseText || "An unexpected server error occurred.");
        // If parsing fails but response was ok (unlikely for login), treat as unexpected
        throw new Error("Received an unreadable response from the server.");
      }

      if (!response.ok) {
        console.error("Login Failed Data from Backend:", data);
        throw new Error(
          data?.error || data?.message || // Prefer backend's error message
          (response.status === 401 ? 'Invalid email or password.' : 'Login failed. Please check credentials or server status.')
        );
      }
      
      // Log the data received from backend before setting user state
      console.log("LoginScreen: Full response data from backend login:", JSON.stringify(data, null, 2));
      
      // Call setUser (which is updateUser from App.js)
      // This will update the 'user' state in App.js, triggering the conditional navigator switch.
      await setUser(data); // Assuming 'data' is the complete user object {id, username, email, role}

      // REMOVED explicit navigation. App.js conditional rendering will handle the switch.
      // navigation.reset({ index: 0, routes: [{ name: 'Home' }] }); 
      // navigation.navigate('Home');

    } catch (error) {
      console.error('Login Catch Error:', error);
      Alert.alert('Login Error', error.message || 'An unexpected error occurred during login.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Image
        source={logoImage} 
        style={styles.logo}
      />
      <Text style={[styles.title, { color: colors.primary }]}>Milgaya!</Text>
      <Text style={styles.subtitle}>Find what's lost, return what's found.</Text>

      <TextInput
        label="Email"
        value={email}
        onChangeText={setEmail}
        mode="outlined"
        style={styles.input}
        keyboardType="email-address"
        autoCapitalize="none"
        left={<TextInput.Icon icon="email-outline" />}
        theme={{ roundness: 8 }}
      />
      <TextInput
        label="Password"
        value={password}
        onChangeText={setPassword}
        mode="outlined"
        style={styles.input}
        secureTextEntry={!showPassword}
        right={
          <TextInput.Icon
            icon={showPassword ? "eye-off-outline" : "eye-outline"}
            onPress={() => setShowPassword(!showPassword)}
          />
        }
        left={<TextInput.Icon icon="lock-outline" />}
        theme={{ roundness: 8 }}
      />
      <Button
        mode="contained"
        onPress={handleLogin}
        loading={isLoading}
        disabled={isLoading}
        style={styles.button}
        labelStyle={styles.buttonLabel}
        icon={isLoading ? () => <PaperActivityIndicator animating={true} size="small" color={colors.onPrimary} /> : "login-variant"}
        theme={{ roundness: 8 }}
      >
        {/* Ensure button text is wrapped in Text for Paper v5 */}
        <Text>{isLoading ? 'LOGGING IN...' : 'LOG IN'}</Text> 
      </Button>
      <View style={styles.footer}>
        <Text style={styles.footerText}>Don't have an account?</Text>
        <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
          <Text style={[styles.linkButton, { color: colors.primary }]}>Register Now</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

// Styles (ensure these are complete and correct for your LoginScreen)
const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingVertical: 20,
    justifyContent: 'center',
    backgroundColor: '#f8f9fa', 
  },
  logo: {
    width: 100,
    height: 100,
    alignSelf: 'center',
    marginBottom: 15,
    resizeMode: 'contain',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    color: '#6c757d', 
    marginBottom: 30,
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
  footer: {
    marginTop: 30,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  footerText: {
    color: '#495057',
    fontSize: 14,
    marginRight: 5,
  },
  linkButton: {
    fontWeight: 'bold',
    fontSize: 14,
  }
});

export default LoginScreen;
