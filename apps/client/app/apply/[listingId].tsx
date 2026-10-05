import { useEffect, useMemo, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  EMPLOYMENT_LABEL, GUARANTEE_LABEL, formatPrice, maskCpf, maskPhoneBR, type ApplicationInput,
} from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import {
  errorText, useApplications, useDocuments, useListing, useProfile, useSaveProfile, useSubmitApplication,
} from '@/lib/queries';
import {
  emptyProfileForm, maskMoney, onlyDigits, profileToForm, validateProfileForm, validateProposalForm,
  type FieldErrors, type ProfileField, type ProfileFormValues, type ProposalField, type ProposalFormValues,
} from '@/lib/forms';
import { formatDateBR } from '@/lib/slots';
import { isRental, listingRef, locationLabel, priceLabel } from '@/lib/listing';
import { ProfileForm } from '@/components/ProfileForm';
import { ProposalForm } from '@/components/ProposalForm';
import { DocumentChecklist, missingRequiredDocs } from '@/components/DocumentChecklist';
import { WizardSteps } from '@/components/WizardSteps';
import { LoginPrompt } from '@/components/LoginPrompt';
import { Icon } from '@/components/Icon';
import { Badge, Button, ErrorBox, Loading } from '@/components/ui';

type Step = 0 | 1 | 2 | 3;

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.reviewRow}>
      <Text style={type.small}>{label}</Text>
      <Text style={[type.body, { fontWeight: '700', flexShrink: 1, textAlign: 'right' }]}>{value}</Text>
    </View>
  );
}

export default function ApplyScreen() {
  const { listingId } = useLocalSearchParams<{ listingId: string }>();
  const id = Number(listingId);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const listing = useListing(id);
  const profile = useProfile();
  const docs = useDocuments();
  const apps = useApplications();
  const saveProfile = useSaveProfile();
  const submit = useSubmitApplication();
  const scrollRef = useRef<ScrollView>(null);

  const [step, setStep] = useState<Step>(0);
  const [submitted, setSubmitted] = useState(false);
  const [profileForm, setProfileForm] = useState<ProfileFormValues>(() => emptyProfileForm(user?.email ?? ''));
  const [profileErrors, setProfileErrors] = useState<FieldErrors<ProfileField>>({});
  const [proposal, setProposal] = useState<ProposalFormValues>({ intent: 'rent', offered_price: '', guarantee_type: null, move_in_date: '', message: '' });
  const [proposalErrors, setProposalErrors] = useState<FieldErrors<ProposalField>>({});
  const [payload, setPayload] = useState<ApplicationInput | null>(null);
  const prefilled = useRef({ profile: false, listing: false });

  // Prefill cadastro once the profile query resolves.
  useEffect(() => {
    if (prefilled.current.profile || profile.isLoading || !user) return;
    prefilled.current.profile = true;
    setProfileForm(profileToForm(profile.data ?? null, user.email ?? '', (user.user_metadata?.full_name as string | undefined) ?? ''));
  }, [profile.isLoading, profile.data, user]);

  // Prefill proposal with the asking price / deal type.
  useEffect(() => {
    if (prefilled.current.listing || !listing.data) return;
    prefilled.current.listing = true;
    setProposal((p) => ({ ...p, intent: isRental(listing.data) ? 'rent' : 'buy', offered_price: maskMoney(String(listing.data.price)) }));
  }, [listing.data]);

  const openApp = useMemo(
    () => (apps.data ?? []).find((a) => a.listing_id === id && ['submitted', 'under_review', 'docs_requested'].includes(a.status)),
    [apps.data, id],
  );

  const goTo = (s: Step) => {
    setStep(s);
    scrollRef.current?.scrollTo({ y: 0, animated: false });
  };

  if (!user) return <LoginPrompt icon="doc" title="Faça sua proposta" text="Entre na sua conta para enviar uma proposta por este imóvel." />;
  if (listing.isLoading || profile.isLoading) return <Loading label="Preparando sua proposta…" />;
  if (!listing.data) return <ErrorBox message={listing.error ? errorText(listing.error) : 'Imóvel não encontrado.'} onRetry={() => listing.refetch()} />;

  const l = listing.data;
  const monthlyIncome = Number(onlyDigits(profileForm.monthly_income) || '0') || profile.data?.monthly_income || null;
  const missingDocs = missingRequiredDocs(docs.data ?? []);

  if (submitted) {
    return (
      <View style={[styles.success, { paddingBottom: insets.bottom + space.xl }]} testID="apply-success">
        <View style={styles.successIcon}>
          <Icon name="check" size={36} color={colors.white} />
        </View>
        <Text style={[type.h1, { textAlign: 'center' }]}>Proposta enviada!</Text>
        <Text style={[type.body, { textAlign: 'center', color: colors.muted }]}>
          Recebemos sua proposta para {l.title}. O corretor analisa em até 2 dias úteis e você acompanha cada etapa na aba Propostas.
        </Text>
        {missingDocs.length ? (
          <View style={{ marginTop: space.md }}>
            <Badge label="Envie os documentos pendentes para agilizar" tone="accent" />
          </View>
        ) : null}
        <View style={{ alignSelf: 'stretch', gap: space.sm, marginTop: space.xl }}>
          <Button variant="accent" title="Acompanhar proposta" onPress={() => router.replace('/propostas')} />
          <Button variant="outline" title="Continuar explorando" onPress={() => router.navigate('/')} />
        </View>
      </View>
    );
  }

  const next = async () => {
    if (step === 0) {
      const v = validateProfileForm(profileForm, true);
      setProfileErrors(v.errors);
      if (!v.ok) return;
      // FK: applications.user_id → client_profiles.user_id, so the profile must exist first.
      await saveProfile.mutateAsync(v.data).then(() => goTo(1)).catch(() => undefined);
    } else if (step === 1) {
      const v = validateProposalForm(l.id, proposal);
      setProposalErrors(v.errors);
      if (!v.ok) return;
      setPayload(v.data);
      goTo(2);
    } else if (step === 2) {
      goTo(3);
    } else if (payload) {
      submit.mutate(payload, { onSuccess: () => setSubmitted(true) });
    }
  };

  const ctaTitle = ['Salvar e continuar', 'Continuar', missingDocs.length ? 'Enviar depois e continuar' : 'Continuar', 'Enviar proposta'][step];
  const busy = saveProfile.isPending || submit.isPending;

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <WizardSteps current={step} />
      <ScrollView ref={scrollRef} contentContainerStyle={{ padding: space.lg, paddingBottom: 130 + insets.bottom }} keyboardShouldPersistTaps="handled">
        <View style={styles.listingCard}>
          <View style={{ flex: 1 }}>
            <Text style={styles.ref}>{listingRef(l.id)}</Text>
            <Text style={type.h3} numberOfLines={1}>{l.title}</Text>
            <Text style={type.small} numberOfLines={1}>{locationLabel(l)}</Text>
          </View>
          <Text style={styles.listPrice}>{priceLabel(l)}</Text>
        </View>

        {openApp ? (
          <View style={styles.notice}>
            <Text style={{ color: colors.coralDark, fontWeight: '700' }}>Você já tem uma proposta em andamento para este imóvel.</Text>
            <Text style={[styles.link]} onPress={() => router.navigate('/propostas')}>
              Ver proposta
            </Text>
          </View>
        ) : null}

        {step === 0 ? (
          <View testID="step-cadastro">
            <Text style={styles.stepTitle}>Confirme seu cadastro</Text>
            <Text style={styles.stepText}>Esses dados ficam salvos no seu perfil e servem para todas as propostas.</Text>
            <ProfileForm values={profileForm} onChange={setProfileForm} errors={profileErrors} />
            {saveProfile.error ? <ErrorBox message={errorText(saveProfile.error)} /> : null}
          </View>
        ) : null}

        {step === 1 ? (
          <View testID="step-proposta">
            <Text style={styles.stepTitle}>Sua proposta</Text>
            <Text style={styles.stepText}>Pode oferecer o valor anunciado ou negociar. O proprietário recebe tudo pelo corretor.</Text>
            <ProposalForm values={proposal} onChange={setProposal} errors={proposalErrors} askingPrice={l.price} currency={l.currency} monthlyIncome={monthlyIncome} />
          </View>
        ) : null}

        {step === 2 ? (
          <View testID="step-documentos">
            <Text style={styles.stepTitle}>Documentos</Text>
            <Text style={styles.stepText}>Envie PDF ou foto. Seus arquivos ficam protegidos e só a equipe REGLA tem acesso.</Text>
            {docs.isLoading ? <Loading /> : <DocumentChecklist documents={docs.data ?? []} />}
          </View>
        ) : null}

        {step === 3 && payload ? (
          <View testID="step-revisao">
            <Text style={styles.stepTitle}>Revise e envie</Text>
            <View style={styles.reviewCard}>
              <View style={styles.reviewHead}>
                <Text style={type.h3}>Proposta</Text>
                <Text style={styles.link} onPress={() => goTo(1)}>Editar</Text>
              </View>
              <Row label="Modalidade" value={payload.intent === 'rent' ? 'Aluguel' : 'Compra'} />
              <Row label={payload.intent === 'rent' ? 'Aluguel mensal' : 'Valor ofertado'} value={formatPrice(payload.offered_price, l.currency)} />
              {payload.intent === 'rent' ? <Row label="Garantia" value={payload.guarantee_type ? GUARANTEE_LABEL[payload.guarantee_type] : '—'} /> : null}
              <Row label="Data desejada" value={payload.move_in_date ? formatDateBR(payload.move_in_date) : 'A combinar'} />
              {payload.message ? <Text style={[type.small, { marginTop: space.sm }]}>“{payload.message}”</Text> : null}
            </View>
            <View style={styles.reviewCard}>
              <View style={styles.reviewHead}>
                <Text style={type.h3}>Cadastro</Text>
                <Text style={styles.link} onPress={() => goTo(0)}>Editar</Text>
              </View>
              <Row label="Nome" value={profileForm.full_name} />
              <Row label="CPF" value={maskCpf(profileForm.cpf)} />
              <Row label="Celular" value={maskPhoneBR(profileForm.phone)} />
              <Row label="Ocupação" value={profileForm.employment_type ? EMPLOYMENT_LABEL[profileForm.employment_type] : '—'} />
              <Row label="Renda mensal" value={monthlyIncome ? formatPrice(monthlyIncome, 'BRL') : '—'} />
              <Row label="Moradores" value={`${profileForm.residents}${profileForm.has_pets ? ' · com pets' : ''}`} />
            </View>
            <View style={styles.reviewCard}>
              <View style={styles.reviewHead}>
                <Text style={type.h3}>Documentos</Text>
                <Text style={styles.link} onPress={() => goTo(2)}>Editar</Text>
              </View>
              {missingDocs.length ? (
                <Badge label={`${missingDocs.length} obrigatório${missingDocs.length > 1 ? 's' : ''} pendente${missingDocs.length > 1 ? 's' : ''}`} tone="accent" />
              ) : (
                <Badge label="Documentos obrigatórios enviados" tone="success" />
              )}
              <Text style={[type.small, { marginTop: space.sm }]}>{(docs.data ?? []).length} arquivo(s) no seu perfil.</Text>
            </View>
            <Text style={[type.small, { marginTop: space.sm }]}>
              Ao enviar, você autoriza a REGLA a compartilhar seu cadastro com o proprietário para análise desta proposta.
            </Text>
            {submit.error ? <ErrorBox message={errorText(submit.error)} /> : null}
          </View>
        ) : null}
      </ScrollView>

      <View style={[styles.bar, { paddingBottom: insets.bottom + space.md }]}>
        {step > 0 ? <Button variant="outline" title="Voltar" onPress={() => goTo((step - 1) as Step)} style={{ minWidth: 100 }} testID="btn-back" /> : null}
        <Button variant="accent" title={ctaTitle} loading={busy} onPress={next} style={{ flex: 1 }} testID="btn-next" />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  listingCard: { flexDirection: 'row', alignItems: 'center', gap: space.md, backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, ...shadow },
  ref: { fontSize: 11, fontWeight: '800', color: colors.coral, letterSpacing: 0.8 },
  listPrice: { fontSize: 15, fontWeight: '800', color: colors.navy },
  notice: { marginTop: space.md, backgroundColor: colors.coralSoft, borderRadius: radius.md, padding: space.md, gap: 4 },
  link: { color: colors.coral, fontWeight: '800' },
  stepTitle: { ...type.h1, marginTop: space.xl },
  stepText: { ...type.body, color: colors.muted, marginTop: 4, marginBottom: space.md },
  reviewCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, marginTop: space.md, ...shadow },
  reviewHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: space.sm },
  reviewRow: { flexDirection: 'row', justifyContent: 'space-between', gap: space.md, paddingVertical: 6 },
  bar: { position: 'absolute', left: 0, right: 0, bottom: 0, flexDirection: 'row', gap: space.md, paddingHorizontal: space.lg, paddingTop: space.md, backgroundColor: colors.white, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.line },
  success: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: space.xxl, gap: space.sm, backgroundColor: colors.bg },
  successIcon: { width: 80, height: 80, borderRadius: 40, backgroundColor: colors.success, alignItems: 'center', justifyContent: 'center', marginBottom: space.md },
});
