import React, { useEffect, useState } from 'react';
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import axios from 'axios';

export default function TestScreen() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get('http://10.7.246.74:8080/api/user/5')
      .then(response => {
        console.log('API Response:', response); // Log the full response for debugging
        setData(response.data);
        setLoading(false);
      })
      .catch(err => {
        console.error('API Error:', err); // Log the error for debugging
        setError(err.message);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>Error: {error}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>User Data</Text>
      {data && (
        <>
          <Text>ID: {data.id}</Text>
          <Text>Name: {data.username}</Text> {/* Ensure 'username' exists in response */}
          <Text>Email: {data.email}</Text>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  error: {
    color: 'red',
  },
});
