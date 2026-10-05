import { router } from 'expo-router';
import { View } from 'react-native';
import { colors } from '@/theme';
import { Button, EmptyState } from '@/components/ui';

export default function NotFound() {
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <EmptyState icon="search" title="Página não encontrada" text="O link pode estar quebrado ou o imóvel saiu do ar." action={<Button title="Ir para a busca" onPress={() => router.replace('/')} />} />
    </View>
  );
}
