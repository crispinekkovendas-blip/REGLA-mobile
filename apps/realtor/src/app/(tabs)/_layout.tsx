import { Tabs } from 'expo-router';
import { Platform, type ColorValue } from 'react-native';
import { TabIcon, type TabIconName } from '../../components/TabIcon';
import { colors } from '../../theme';

const icon = (name: TabIconName) => ({ color }: { color: ColorValue }) => <TabIcon name={name} color={color} />;

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: colors.navy },
        headerTintColor: colors.white,
        headerTitleStyle: { fontWeight: '800', fontSize: 18 },
        headerShadowVisible: false,
        tabBarStyle: {
          backgroundColor: colors.navy,
          borderTopColor: colors.navyMid,
          height: Platform.OS === 'web' ? 60 : undefined,
        },
        tabBarActiveTintColor: colors.coral,
        tabBarInactiveTintColor: colors.onNavyMuted,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '700' },
        sceneStyle: { backgroundColor: colors.navySoft },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Início', headerTitle: 'REGLA Corretor', tabBarIcon: icon('home') }} />
      <Tabs.Screen name="leads" options={{ title: 'Leads', tabBarIcon: icon('leads') }} />
      <Tabs.Screen name="propostas" options={{ title: 'Propostas', tabBarIcon: icon('proposals') }} />
      <Tabs.Screen name="agenda" options={{ title: 'Agenda', tabBarIcon: icon('agenda') }} />
      <Tabs.Screen name="imoveis" options={{ title: 'Imóveis', tabBarIcon: icon('listings') }} />
    </Tabs>
  );
}
