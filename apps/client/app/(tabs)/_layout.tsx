import { Platform, type ColorValue } from 'react-native';
import { Tabs } from 'expo-router/js-tabs';
import { Icon, type IconName } from '@/components/Icon';
import { colors } from '@/theme';

const tabIcon = (name: IconName, activeName?: IconName) =>
  function TabIcon({ color, focused }: { color: ColorValue; focused: boolean }) {
    return <Icon name={focused && activeName ? activeName : name} size={22} color={color} />;
  };

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.coral,
        tabBarInactiveTintColor: colors.muted,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '700' },
        tabBarStyle: {
          backgroundColor: colors.white,
          borderTopColor: colors.line,
          height: Platform.OS === 'web' ? 64 : undefined,
          paddingTop: 6,
        },
        sceneStyle: { backgroundColor: colors.bg },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Buscar', tabBarIcon: tabIcon('search') }} />
      <Tabs.Screen name="favoritos" options={{ title: 'Favoritos', tabBarIcon: tabIcon('heart-outline', 'heart') }} />
      <Tabs.Screen name="visitas" options={{ title: 'Visitas', tabBarIcon: tabIcon('calendar') }} />
      <Tabs.Screen name="propostas" options={{ title: 'Propostas', tabBarIcon: tabIcon('doc') }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil', tabBarIcon: tabIcon('user') }} />
    </Tabs>
  );
}
