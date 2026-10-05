import { useMemo } from 'react';
import { FlatList, RefreshControl, View } from 'react-native';
import { router } from 'expo-router';
import { colors, space } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useFavoriteIds, useListings } from '@/lib/queries';
import { useFavorites } from '@/lib/useFavorites';
import { ListingCard } from '@/components/ListingCard';
import { LoginPrompt } from '@/components/LoginPrompt';
import { ScreenTitle } from '@/components/ScreenTitle';
import { Button, EmptyState, ErrorBox, Loading } from '@/components/ui';

export default function FavoritesScreen() {
  const { user } = useAuth();
  const favIds = useFavoriteIds();
  const all = useListings({});
  const { isFavorite, toggle } = useFavorites();

  const items = useMemo(() => {
    const ids = new Set(favIds.data ?? []);
    return (all.data ?? []).filter((l) => ids.has(l.id));
  }, [favIds.data, all.data]);

  if (!user) {
    return <LoginPrompt icon="heart" title="Seus favoritos" text="Entre para salvar imóveis e acessá-los de qualquer aparelho." />;
  }

  const loading = favIds.isLoading || all.isLoading;
  const error = favIds.error || all.error;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <FlatList
        data={items}
        keyExtractor={(l) => String(l.id)}
        ListHeaderComponent={
          <View>
            <ScreenTitle title="Favoritos" subtitle={items.length ? `${items.length} imóve${items.length === 1 ? 'l salvo' : 'is salvos'}` : undefined} />
            {error ? <View style={{ paddingHorizontal: space.lg }}><ErrorBox message={errorText(error)} onRetry={() => { favIds.refetch(); all.refetch(); }} /></View> : null}
          </View>
        }
        contentContainerStyle={{ paddingBottom: space.xxxl }}
        renderItem={({ item }) => (
          <View style={{ paddingHorizontal: space.lg }}>
            <ListingCard listing={item} compact isFavorite={isFavorite(item.id)} onToggleFavorite={(l) => toggle(l.id)} onPress={(l) => router.push(`/listing/${l.id}`)} />
          </View>
        )}
        ListEmptyComponent={
          loading ? (
            <Loading />
          ) : (
            <EmptyState
              icon="heart-outline"
              title="Nenhum favorito ainda"
              text="Toque no coração dos imóveis que você gostar para guardá-los aqui."
              action={<Button title="Explorar imóveis" variant="accent" onPress={() => router.navigate('/')} />}
            />
          )
        }
        refreshControl={<RefreshControl refreshing={favIds.isRefetching} onRefresh={() => { favIds.refetch(); all.refetch(); }} tintColor={colors.navy} />}
      />
    </View>
  );
}
