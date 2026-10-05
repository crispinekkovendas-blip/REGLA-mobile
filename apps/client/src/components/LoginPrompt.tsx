import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { colors, radius, shadow, space, type } from '@/theme';
import { Icon, type IconName } from './Icon';
import { Button } from './ui';

export function LoginPrompt({ icon = 'user', title, text }: { icon?: IconName; title: string; text: string }) {
  return (
    <View style={styles.wrap}>
      <View style={styles.card}>
        <View style={styles.icon}>
          <Icon name={icon} size={28} color={colors.coral} />
        </View>
        <Text style={[type.h1, { textAlign: 'center' }]}>{title}</Text>
        <Text style={[type.body, { color: colors.muted, textAlign: 'center' }]}>{text}</Text>
        <View style={{ alignSelf: 'stretch', gap: space.sm, marginTop: space.lg }}>
          <Button variant="accent" title="Entrar" onPress={() => router.push('/auth/login')} />
          <Button variant="outline" title="Criar conta grátis" onPress={() => router.push('/auth/signup')} />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, justifyContent: 'center', padding: space.xl, backgroundColor: colors.bg },
  card: { backgroundColor: colors.white, borderRadius: radius.xl, padding: space.xxl, alignItems: 'center', gap: space.sm, ...shadow },
  icon: { width: 64, height: 64, borderRadius: 32, backgroundColor: colors.coralSoft, alignItems: 'center', justifyContent: 'center', marginBottom: space.sm },
});
