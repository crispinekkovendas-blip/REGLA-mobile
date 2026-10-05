import { useState, type ReactNode } from 'react';
import { Linking, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { BROKER_WHATSAPP, maskPhoneBR, waUrl } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { useDocuments, useProfile } from '@/lib/queries';
import { missingRequiredDocs } from '@/components/DocumentChecklist';
import { LoginPrompt } from '@/components/LoginPrompt';
import { ScreenTitle } from '@/components/ScreenTitle';
import { Icon, type IconName } from '@/components/Icon';
import { Badge, Button } from '@/components/ui';

function MenuRow({ icon, title, subtitle, right, onPress, testID }: { icon: IconName; title: string; subtitle?: string; right?: ReactNode; onPress: () => void; testID?: string }) {
  return (
    <Pressable testID={testID} onPress={onPress} accessibilityRole="button" style={({ pressed }) => [styles.menuRow, pressed && { backgroundColor: colors.navySoft }]}>
      <View style={styles.menuIcon}>
        <Icon name={icon} size={20} color={colors.navy} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={type.h3}>{title}</Text>
        {subtitle ? <Text style={type.small}>{subtitle}</Text> : null}
      </View>
      {right}
      <Icon name="chevron-right" size={14} color={colors.faint} />
    </Pressable>
  );
}

/** Rough completeness of the cadastro (fields the broker needs for analysis). */
function completeness(p: ReturnType<typeof useProfile>['data']): number {
  if (!p) return 0;
  const checks = [p.full_name, p.email, p.phone, p.cpf, p.birth_date, p.employment_type, p.occupation, p.monthly_income];
  return Math.round((checks.filter((v) => v !== null && v !== undefined && v !== '').length / checks.length) * 100);
}

export default function ProfileScreen() {
  const { user, signOut } = useAuth();
  const profile = useProfile();
  const docs = useDocuments();
  const [leaving, setLeaving] = useState(false);

  if (!user) return <LoginPrompt icon="user" title="Sua conta REGLA" text="Entre para montar seu cadastro uma única vez e usar em todas as propostas." />;

  const pct = completeness(profile.data);
  const missing = missingRequiredDocs(docs.data ?? []);
  const name = profile.data?.full_name || (user.user_metadata?.full_name as string | undefined) || user.email || '';
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('') || 'R';

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ paddingBottom: space.xxxl }}>
      <ScreenTitle title="Perfil" />
      <View style={{ paddingHorizontal: space.lg, gap: space.lg }}>
        <View style={styles.idCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{initials}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={[type.h2, { color: colors.white }]} numberOfLines={1}>
              {name}
            </Text>
            <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 13 }} numberOfLines={1}>
              {user.email}
              {profile.data?.phone ? ` · ${maskPhoneBR(profile.data.phone)}` : ''}
            </Text>
          </View>
        </View>

        <View style={styles.progressCard}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text style={type.h3}>Cadastro {pct}% completo</Text>
            {pct === 100 ? <Badge label="Pronto para propostas" tone="success" /> : <Badge label="Incompleto" tone="accent" />}
          </View>
          <View style={styles.bar}>
            <View style={[styles.barFill, { width: `${Math.max(pct, 4)}%` }]} />
          </View>
          <Text style={type.small}>Um cadastro completo agiliza a análise da sua proposta pelo proprietário.</Text>
        </View>

        <View style={styles.menu}>
          <MenuRow
            testID="menu-cadastro"
            icon="user"
            title="Meu cadastro"
            subtitle="Dados pessoais, renda e moradia"
            onPress={() => router.push('/profile/edit')}
          />
          <View style={styles.sep} />
          <MenuRow
            icon="doc"
            title="Meus documentos"
            subtitle={docs.data ? `${docs.data.length} enviado${docs.data.length === 1 ? '' : 's'}` : 'RG, renda, residência'}
            right={missing.length && docs.data ? <Badge label={`${missing.length} pendente${missing.length > 1 ? 's' : ''}`} tone="accent" /> : null}
            onPress={() => router.push('/profile/documents')}
          />
          <View style={styles.sep} />
          <MenuRow icon="calendar" title="Minhas visitas" onPress={() => router.navigate('/visitas')} />
          <View style={styles.sep} />
          <MenuRow icon="chat" title="Falar com um corretor" subtitle={`WhatsApp ${maskPhoneBR(BROKER_WHATSAPP.slice(2))}`} onPress={() => Linking.openURL(waUrl('Olá! Vim pelo app REGLA e gostaria de ajuda.'))} />
        </View>

        <Button
          variant="danger"
          title="Sair da conta"
          loading={leaving}
          onPress={async () => {
            setLeaving(true);
            try {
              await signOut();
            } finally {
              setLeaving(false);
            }
          }}
        />
        <Text style={[type.small, { textAlign: 'center' }]}>REGLA Imóveis · imoveisregla.com.br</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  idCard: { flexDirection: 'row', alignItems: 'center', gap: space.lg, backgroundColor: colors.navy, borderRadius: radius.lg, padding: space.xl },
  avatar: { width: 56, height: 56, borderRadius: 28, backgroundColor: colors.coral, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.white, fontWeight: '800', fontSize: 20 },
  progressCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, gap: space.sm, ...shadow },
  bar: { height: 8, borderRadius: 4, backgroundColor: colors.navySoft, overflow: 'hidden' },
  barFill: { height: 8, borderRadius: 4, backgroundColor: colors.coral },
  menu: { backgroundColor: colors.white, borderRadius: radius.lg, overflow: 'hidden', ...shadow },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: space.md, padding: space.lg },
  menuIcon: { width: 40, height: 40, borderRadius: 12, backgroundColor: colors.navySoft, alignItems: 'center', justifyContent: 'center' },
  sep: { height: StyleSheet.hairlineWidth, backgroundColor: colors.line, marginLeft: 72 },
});
