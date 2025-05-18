    import Color from 'color';
import { ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import { Avatar, Card, Button as PaperButton, Text, Title, useTheme } from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

    export default function HomeScreen({ navigation, setUser, user }) {
      const { colors } = useTheme();

      const handleLogout = () => { setUser(null); };

      const menuItems = [
        { title: "Report Lost Item", icon: "magnify-scan", action: () => navigation.navigate('SubmitItem', { type: 'lost', userId: user?.id }), backgroundColor: colors.errorContainer, iconColor: colors.onErrorContainer, description: "Submit details of an item you've lost." },
        { title: "Report Found Item", icon: "hand-heart-outline", action: () => navigation.navigate('SubmitItem', { type: 'found', userId: user?.id }), backgroundColor: colors.primaryContainer, iconColor: colors.onPrimaryContainer, description: "Report an item you've found." },
        { title: "Browse Items", icon: "format-list-bulleted-square", action: () => navigation.navigate('ItemList', { currentUser: user }), backgroundColor: colors.tertiaryContainer || '#E8DEF8', iconColor: colors.onTertiaryContainer || '#4A148C', description: "View all reported items." },
      ];

      // Add Admin button conditionally
      if (user && user.role === 'admin') {
        menuItems.push({
          title: "Admin Claim Review",
          icon: "shield-check-outline",
          action: () => navigation.navigate('AdminReviewScreen'), // Navigate to a new screen
          backgroundColor: colors.surfaceVariant, // A different color for admin
          iconColor: colors.onSurfaceVariant,
          description: "Review claims forwarded by finders."
        });
      }
      
      // Pre-calculate description text colors
      const processedMenuItems = menuItems.map(item => ({
          ...item,
          descriptionTextColor: item.iconColor ? Color(item.iconColor).alpha(0.7).rgb().string() : colors.onSurfaceVariant
      }));


      const userName = user?.username || user?.email || "User";

      return (
        <ScrollView contentContainerStyle={styles.container}>
          <Card style={styles.headerCard}>
            <Card.Content style={styles.headerCardContent}>
              <View style={styles.headerTextContainer}>
                <Title style={styles.welcomeTitle}><Text>Welcome, {userName}!</Text></Title>
                <Text style={styles.headerSubtitle}>How can we help you today?</Text>
              </View>
              <Avatar.Image size={60} source={require('../assets/images/favicon.png')} style={{ backgroundColor: colors.surfaceDisabled || colors.surfaceVariant }}/>
            </Card.Content>
          </Card>

          <Text style={styles.sectionTitle}>Quick Actions</Text>
          <View style={styles.menuGrid}>
            {processedMenuItems.map((item, index) => (
              <TouchableOpacity key={index} style={[styles.touchableMenuCard, { backgroundColor: item.backgroundColor }]} onPress={item.action} activeOpacity={0.7}>
                <View style={styles.menuCardInner}>
                  <Icon name={item.icon} size={32} color={item.iconColor || colors.primary} style={styles.menuIcon} />
                  <Text style={[styles.menuTitle, { color: item.iconColor || colors.primary }]}>{item.title}</Text>
                  <Text style={[styles.menuDescription, { color: item.descriptionTextColor }]}>{item.description}</Text>
                </View>
              </TouchableOpacity>
            ))}
          </View>

          <PaperButton mode="outlined" onPress={handleLogout} icon="logout" style={styles.logoutButton} labelStyle={[styles.logoutButtonLabel, {color: colors.error}]} textColor={colors.error}>
            <Text>Logout</Text>
          </PaperButton>
        </ScrollView>
      );
    }

    // Styles (ensure they are complete and match your previous version)
    const styles = StyleSheet.create({
      container: { flexGrow: 1, padding: 16, backgroundColor: '#f0f2f5', },
      headerCard: { marginBottom: 24, elevation: 3, borderRadius: 16, backgroundColor: '#ffffff', },
      headerCardContent: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 20, paddingHorizontal: 16, },
      headerTextContainer: { flex: 1, marginRight: 10, },
      welcomeTitle: { fontSize: 22, fontWeight: 'bold', color: '#343a40', },
      headerSubtitle: { fontSize: 15, color: '#6c757d', marginTop: 2, },
      sectionTitle: { fontSize: 18, fontWeight: '600', color: '#495057', marginBottom: 16, marginLeft: 4, },
      menuGrid: { /* Styles for list view */ },
      touchableMenuCard: { borderRadius: 12, elevation: 2, marginBottom: 16, overflow: 'hidden', },
      menuCardInner: { padding: 16, alignItems: 'center', },
      menuIcon: { marginBottom: 10, },
      menuTitle: { fontSize: 17, fontWeight: 'bold', textAlign: 'center', marginBottom: 5, },
      menuDescription: { fontSize: 13, textAlign: 'center', minHeight: 35, paddingHorizontal: 5, },
      logoutButton: { marginTop: 30, marginBottom: 10, borderWidth: 1, borderRadius: 8, paddingVertical: 6, },
      logoutButtonLabel: { fontSize: 16, fontWeight: '600', }
    });
    