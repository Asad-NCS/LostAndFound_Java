import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useState } from 'react';
import { Alert, FlatList, Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button, Card, Chip, ActivityIndicator as PaperActivityIndicator, Searchbar, Title, useTheme } from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const API_BASE_URL = 'http://10.7.41.239:8080';

export default function ItemListScreen({ navigation, route }) { // Added route to get currentUser
  const currentUser = route.params?.currentUser; // <<< RECEIVE currentUser from HomeScreen
  
  const [items, setItems] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('all');
  const [isLoading, setIsLoading] = useState(true);
  const { colors } = useTheme();

  const fetchItems = useCallback(async (currentFilter) => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();
      if (currentFilter && currentFilter !== 'all') {
        params.append('status', currentFilter);
      }
      // If your backend supports search, uncomment and use this
      // if (searchQuery.trim()) {
      //   params.append('search', searchQuery.trim());
      // }
      
      const queryString = params.toString();
      const url = `${API_BASE_URL}/api/items${queryString ? `?${queryString}` : ''}`;
      
      const res = await fetch(url);
      if (!res.ok) {
        const errorData = await res.json().catch(() => ({ message: 'Server returned an error.' }));
        throw new Error(errorData.message || `Failed to fetch items (status: ${res.status})`);
      }
      const data = await res.json();
      setItems(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Fetch Items Error:', error);
      Alert.alert('Error Loading Items', error.message || 'Could not load items. Please try again.');
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, []); // Removed searchQuery from dependencies here; search can be client-side for now or trigger refetch via button

  useFocusEffect(
    useCallback(() => {
      fetchItems(activeFilter);
    }, [activeFilter, fetchItems])
  );
  
  // Client-side filtering for search (if backend doesn't handle it)
  const displayedItems = items.filter(item =>
    item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (item.description && item.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const renderItem = ({ item }) => {
    let imageUri = null;
    if (item.imageUrl) {
      if (item.imageUrl.startsWith('http')) {
        imageUri = item.imageUrl;
      } else if (item.imageUrl.startsWith('/api/items/images/')) {
        imageUri = `${API_BASE_URL}${item.imageUrl}`;
      } else { 
        imageUri = `${API_BASE_URL}/api/items/images/${item.imageUrl}`;
      }
    }

    return (
      <TouchableOpacity
        style={styles.itemTouchable}
        onPress={() => navigation.navigate('ItemDetail', {
          item: item,
          currentUser: currentUser // <<< PASS currentUser HERE
        })}
        activeOpacity={0.7}
      >
        <Card style={styles.itemCard}>
          <View style={styles.itemContentRow}>
            {imageUri ? (
              <Image
                source={{ uri: imageUri, cache: 'force-cache' }}
                style={styles.itemImage}
                resizeMode="cover"
                onError={(e) => console.warn('Item List Image load error:', e.nativeEvent.error, 'URI:', imageUri)}
              />
            ) : (
              <View style={[styles.itemImage, styles.imagePlaceholder]}>
                <Icon name="camera-off-outline" size={30} color={colors.onSurfaceVariant} />
              </View>
            )}
            <View style={styles.textContainer}>
              <Title style={styles.itemTitle} numberOfLines={2}>{item.title}</Title>
              <View style={styles.detailRow}>
                <Icon name="map-marker-outline" size={16} color={colors.onSurfaceVariant} />
                <Text style={styles.itemLocation} numberOfLines={1}>{item.location}</Text>
              </View>
              <View style={styles.detailRow}>
                 <Chip 
                    icon={item.isLost ? "alert-circle-outline" : "check-circle-outline"}
                    selectedColor={item.isLost ? colors.onErrorContainer : colors.onPrimaryContainer}
                    style={[
                        styles.statusChip, 
                        { backgroundColor: item.isLost ? colors.errorContainer : colors.primaryContainer }
                    ]}
                    textStyle={styles.chipText}
                 >
                    {item.isLost ? 'Lost' : 'Found'}{item.claimed ? ' (Claimed)' : ''}
                 </Chip>
              </View>
            </View>
          </View>
        </Card>
      </TouchableOpacity>
    );
  };

  if (isLoading && !items.length) {
    return (
      <View style={styles.fullScreenLoader}>
        <PaperActivityIndicator size="large" animating={true} />
        <Text style={styles.loaderText}>Loading items...</Text>
      </View>
    );
  }

  return (
    <View style={styles.screenContainer}>
      <Searchbar
        placeholder="Search items..."
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={styles.searchBar}
        iconColor={colors.primary}
        // onIconPress={() => fetchItems(activeFilter)} // Uncomment if backend search is implemented
        // onSubmitEditing={() => fetchItems(activeFilter)} // Uncomment if backend search is implemented
        theme={{ roundness: 8 }}
      />
      <View style={styles.filterContainer}>
        <Button
          mode={activeFilter === 'all' ? 'contained' : 'outlined'}
          onPress={() => setActiveFilter('all')}
          style={styles.filterButton}
          labelStyle={styles.filterLabel}
          theme={{ roundness: 18 }}
        >
          All
        </Button>
        <Button
          mode={activeFilter === 'lost' ? 'contained' : 'outlined'}
          onPress={() => setActiveFilter('lost')}
          style={styles.filterButton}
          labelStyle={styles.filterLabel}
          buttonColor={activeFilter === 'lost' ? colors.errorContainer : undefined}
          textColor={activeFilter === 'lost' ? colors.onErrorContainer : colors.error}
          theme={{ roundness: 18 }}
        >
          Lost
        </Button>
        <Button
          mode={activeFilter === 'found' ? 'contained' : 'outlined'}
          onPress={() => setActiveFilter('found')}
          style={styles.filterButton}
          labelStyle={styles.filterLabel}
          buttonColor={activeFilter === 'found' ? colors.primaryContainer : undefined}
          textColor={activeFilter === 'found' ? colors.onPrimaryContainer : colors.primary}
          theme={{ roundness: 18 }}
        >
          Found
        </Button>
      </View>

      {isLoading && items.length > 0 && <PaperActivityIndicator animating={true} style={{marginVertical: 10}}/>}

      <FlatList
        data={displayedItems} // Changed to displayedItems for client-side search
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        contentContainerStyle={styles.listContentContainer}
        ListEmptyComponent={
          !isLoading && (
            <View style={styles.emptyListComponent}>
              <Icon name="database-search-outline" size={60} color={colors.onSurfaceDisabled} />
              <Text style={styles.emptyListText}>No items found.</Text>
              {searchQuery ? 
                <Text style={styles.emptyListSubtext}>Try a different search term.</Text> :
                <Text style={styles.emptyListSubtext}>Try adjusting your filters or check back later.</Text>
              }
            </View>
          )
        }
        onRefresh={() => fetchItems(activeFilter)}
        refreshing={isLoading}
      />
    </View>
  );
}

const styles = StyleSheet.create({ /* ... Your ItemListScreen styles from before ... */ 
    screenContainer: { flex: 1, backgroundColor: '#f0f2f5', },
    searchBar: { marginHorizontal: 16, marginTop: 16, marginBottom: 8, elevation: 2, },
    filterContainer: { flexDirection: 'row', justifyContent: 'space-around', paddingHorizontal: 16, marginBottom: 16, },
    filterButton: { flex: 1, marginHorizontal: 4, borderWidth: 1, },
    filterLabel: { fontSize: 13, fontWeight: '500', },
    listContentContainer: { paddingHorizontal: 16, paddingBottom: 16, },
    itemTouchable:{ marginBottom: 12, },
    itemCard: { borderRadius: 12, elevation: 2, backgroundColor: '#fff', },
    itemContentRow: { flexDirection: 'row', padding: 12, },
    itemImage: { width: 90, height: 90, borderRadius: 8, marginRight: 12, backgroundColor: '#e9ecef', },
    imagePlaceholder: { justifyContent: 'center', alignItems: 'center', },
    textContainer: { flex: 1, justifyContent: 'center', },
    itemTitle: { fontSize: 17, fontWeight: 'bold', marginBottom: 6, color: '#343a40', lineHeight: 22, },
    detailRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 4, },
    itemLocation: { fontSize: 14, color: '#6c757d', marginLeft: 6, flexShrink: 1, },
    statusChip: { height: 28, alignItems: 'center', justifyContent: 'center', borderRadius: 16, paddingHorizontal: 1, marginTop: 4, },
    chipText: { fontSize: 12, fontWeight: '500', marginHorizontal: 6, },
    fullScreenLoader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f0f2f5', },
    loaderText: { marginTop: 10, fontSize: 16, color: '#6c757d', },
    emptyListComponent: { flex: 1, justifyContent: 'center', alignItems: 'center', marginTop: 50, padding: 20, },
    emptyListText: { fontSize: 18, fontWeight: 'bold', color: '#adb5bd', marginTop: 16, },
    emptyListSubtext: { fontSize: 14, color: '#ced4da', textAlign: 'center', marginTop: 8, }
});