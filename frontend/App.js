import AsyncStorage from '@react-native-async-storage/async-storage'; // For storing user session
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { registerRootComponent } from 'expo';
import { useEffect, useState } from 'react'; // Import React
import { DefaultTheme, Provider as PaperProvider } from 'react-native-paper';

// Import your screens
import AdminReviewScreen from './screens/AdminReviewScreen';
import HomeScreen from './screens/HomeScreen';
import ItemDetailScreen from './screens/ItemDetailScreen';
import ItemListScreen from './screens/ItemListScreen';
import LoginScreen from './screens/LoginScreen';
import SignupScreen from './screens/SignupScreen';
import SubmitItemScreen from './screens/SubmitItemScreen';
import VerifyScreen from './screens/VerifyScreen';

// Your custom theme
const theme = {
  ...DefaultTheme,
  roundness: 8, 
  colors: {
    ...DefaultTheme.colors,
    primary: '#6200EE', 
    secondary: '#03DAC6', 
    tertiary: '#3700B3', 
    error: '#B00020',    
    background: '#f6f6f6', 
    surface: '#ffffff',     
    primaryContainer: '#EADDFF', 
    secondaryContainer: '#CCEFFC',
    tertiaryContainer: '#E8DEF8', 
    errorContainer: '#FFDAD6',
    onPrimary: '#FFFFFF',
    onSecondary: '#000000',
    onTertiary: '#FFFFFF',
    onError: '#FFFFFF',
    onBackground: '#000000',
    onSurface: '#000000',
    onPrimaryContainer: '#21005D', 
    onErrorContainer: '#410002',   
    onSecondaryContainer: '#001F2A',
    onTertiaryContainer: '#1D192B',
    outline: '#79747E',
    surfaceVariant: '#E7E0EC',
    onSurfaceVariant: '#49454F',
    surfaceDisabled: 'rgba(28, 27, 31, 0.12)', 
    onSurfaceDisabled: 'rgba(28, 27, 31, 0.38)',
  },
};

const Stack = createNativeStackNavigator();

export default function App() {
  const [user, setUserState] = useState(null);
  const [isLoading, setIsLoading] = useState(true); 

  // Function to update user state and persist to AsyncStorage
  const updateUser = async (userData) => {
    // <<< CONSOLE LOG 1: What data is passed to updateUser? >>>
    console.log("[App.js] updateUser received userData:", JSON.stringify(userData, null, 2)); 
    try {
      if (userData) {
        await AsyncStorage.setItem('userData', JSON.stringify(userData));
        console.log("[App.js] User data saved to AsyncStorage.");
      } else {
        await AsyncStorage.removeItem('userData');
        console.log("[App.js] User data removed from AsyncStorage.");
      }
      setUserState(userData); // Sets the app-wide user state
    } catch (e) {
      console.error("[App.js] Failed to save/remove user data from storage", e);
    }
  };

  // Check for stored user session on app startup
  useEffect(() => {
    const bootstrapAsync = async () => {
      let storedUserData;
      try {
        storedUserData = await AsyncStorage.getItem('userData');
        if (storedUserData) {
          const parsedUser = JSON.parse(storedUserData);
          // <<< CONSOLE LOG 2: What user data is loaded from storage? >>>
          console.log("[App.js] User loaded from storage on app start:", JSON.stringify(parsedUser, null, 2));
          setUserState(parsedUser);
        } else {
          console.log("[App.js] No user data found in storage.");
        }
      } catch (e) {
        console.error("[App.js] Failed to load user data from storage", e);
      }
      setIsLoading(false);
    };
    
    bootstrapAsync();
    
  }, []);
  

  if (isLoading) { return null; }

  return (
    <PaperProvider theme={theme}>
      <NavigationContainer theme={theme}>
        <Stack.Navigator
          screenOptions={{
            headerStyle: { backgroundColor: theme.colors.primary, },
            headerTintColor: theme.colors.onPrimary,
            headerTitleStyle: { fontWeight: 'bold', },
          }}
        >
          {user ? (
            // Authenticated screens
            // The 'user' object from App.js state is passed as a prop to these screens
            <>
              <Stack.Screen name="Home" options={{ title: 'Dashboard' }}>
                {(props) => <HomeScreen {...props} setUser={updateUser} user={user} />}
              </Stack.Screen>
              <Stack.Screen name="SubmitItem" options={{ title: 'Report an Item' }}>
                 {(props) => <SubmitItemScreen {...props} user={user} />}
              </Stack.Screen>
              <Stack.Screen name="ItemList" options={{ title: 'Browse Items' }}>
                 {/* ItemListScreen receives 'user' prop. It should then pass it as 'currentUser' 
                     to ItemDetailScreen via route.params if ItemDetailScreen needs it that way.
                     Or, ItemDetailScreen can also receive 'user' directly as a prop like others here.
                 */}
                 {(props) => <ItemListScreen {...props} user={user} />}
              </Stack.Screen>
              <Stack.Screen name="ItemDetail" options={{ title: 'Item Details' }}>
                 {/* If ItemDetailScreen needs 'currentUser' from route.params, ItemListScreen must pass it.
                    Alternatively, pass 'user' prop directly like other screens:
                    (props) => <ItemDetailScreen {...props} user={user} />
                    Then, ItemDetailScreen would use `props.user` or destructure `user` from props
                    instead of `route.params.currentUser`.
                    For consistency with how AdminReviewScreen gets 'currentUser', let's pass it directly.
                 */}
                 {(props) => <ItemDetailScreen {...props} currentUser={user} />} 
              </Stack.Screen>
              {/* Conditionally add AdminReviewScreen if user is admin */}
              {user.role === 'admin' && (
                <Stack.Screen 
                    name="AdminReviewScreen" 
                    options={{ title: 'Admin Claim Review' }}
                >
                    {(props) => <AdminReviewScreen {...props} currentUser={user} />}
                </Stack.Screen>
              )}
            </>
          ) : (
            // Auth screens
            <>
              <Stack.Screen name="Login" options={{ headerShown: false }}>
                {(props) => <LoginScreen {...props} setUser={updateUser} />}
              </Stack.Screen>
              <Stack.Screen name="Signup" component={SignupScreen} options={{ title: 'Create Account' }}/>
              <Stack.Screen name="Verify" component={VerifyScreen} options={{ title: 'Verify Account' }}/>
            </>
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </PaperProvider>
  );
}

registerRootComponent(App);