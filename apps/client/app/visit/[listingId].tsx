import { useEffect, useMemo, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { maskPhoneBR, visitSchema } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useBookVisit, useListing, useProfile } from '@/lib/queries';
import { formatVisitDate, nextDays, timeSlots } from '@/lib/slots';
import { onlyDigits, zodFieldErrors, type FieldErrors } from '@/lib/forms';
import { listingRef, locationLabel } from '@/lib/listing';
import { LoginPrompt } from '@/components/LoginPrompt';
import { Icon } from '@/components/Icon';
import { Button, ErrorBox, Field, Loading } from '@/components/ui';

type VisitField = 'visitor_name' | 'visitor_email' | 'visitor_phone' | 'starts_at' | 'notes';

export default function VisitScreen() {
  const { listingId } = useLocalSearchParams<{ listingId: string }>();
  const id = Number(listingId);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const listing = useListing(id);
  const profile = useProfile();
  const book = useBookVisit();

  const days = useMemo(() => nextDays(new Date(), 14), []);
  const [dayKey, setDayKey] = useState(days[0].key);
  const day = days.find((d) => d.key === dayKey) ?? days[0];
  const slots = useMemo(() => timeSlots(day.date, new Date()), [day]);
  const [slotIso, setSlotIso] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<FieldErrors<VisitField>>({});
  const [done, setDone] = useState<string | null>(null);

  // Prefill contact data from the cadastro / account.
  useEffect(() => {
    const p = profile.data;
    setName((v) => v || p?.full_name || ((user?.user_metadata?.full_name as string | undefined) ?? ''));
    setEmail((v) => v || p?.email || user?.email || '');
    setPhone((v) => v || (p?.phone ? maskPhoneBR(p.phone) : ''));
  }, [profile.data, user]);

  // If today has no available slots, jump to tomorrow.
  useEffect(() => {
    if (day.isToday && slots.every((s) => s.disabled) && days[1]) setDayKey(days[1].key);
  }, [day, slots, days]);

  if (!user) return <LoginPrompt icon="calendar" title="Agende sua visita" text="Entre na sua conta para escolher um horário." />;
  if (listing.isLoading) return <Loading />;

  if (done) {
    return (
      <View style={[styles.success, { paddingBottom: insets.bottom + space.xl }]}>
        <View style={styles.successIcon}>
          <Icon name="check" size={34} color={colors.white} />
        </View>
        <Text style={[type.h1, { textAlign: 'center' }]}>Visita solicitada!</Text>
        <Text style={[type.body, { textAlign: 'center', color: colors.muted }]}>
          {formatVisitDate(done)}
          {'\n'}O corretor vai confirmar o horário pelo WhatsApp ou e-mail.
        </Text>
        <View style={{ alignSelf: 'stretch', gap: space.sm, marginTop: space.xl }}>
          <Button variant="accent" title="Ver minhas visitas" onPress={() => router.replace('/visitas')} />
          <Button variant="outline" title="Voltar ao imóvel" onPress={() => router.back()} />
        </View>
      </View>
    );
  }

  const submit = () => {
    const candidate = {
      listing_id: id,
      starts_at: slotIso ?? '',
      visitor_name: name,
      visitor_email: email,
      visitor_phone: onlyDigits(phone) ? onlyDigits(phone) : null,
      notes: notes.trim() ? notes.trim() : null,
    };
    const res = visitSchema.safeParse(candidate);
    const errs: FieldErrors<VisitField> = res.success ? {} : zodFieldErrors<VisitField>(res.error);
    if (!slotIso) errs.starts_at = 'Escolha um horário';
    if (errs.visitor_name) errs.visitor_name = 'Informe seu nome';
    if (errs.visitor_email) errs.visitor_email = 'E-mail inválido';
    if (candidate.visitor_phone && candidate.visitor_phone.length < 10) errs.visitor_phone = 'Telefone inválido';
    setErrors(errs);
    if (!res.success || Object.keys(errs).length) return;
    book.mutate(res.data, { onSuccess: () => setDone(res.data.starts_at) });
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: space.lg, paddingBottom: 120 + insets.bottom }} keyboardShouldPersistTaps="handled">
        {listing.data ? (
          <View style={styles.listingCard}>
            <Text style={styles.ref}>{listingRef(listing.data.id)}</Text>
            <Text style={type.h3} numberOfLines={1}>{listing.data.title}</Text>
            <Text style={type.small}>{locationLabel(listing.data)}</Text>
          </View>
        ) : null}

        <Text style={styles.group}>Escolha o dia</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: space.sm, paddingVertical: 4 }}>
          {days.map((d) => {
            const active = d.key === dayKey;
            return (
              <Pressable
                key={d.key}
                testID={`day-${d.key}`}
                accessibilityRole="button"
                accessibilityState={{ selected: active }}
                onPress={() => {
                  setDayKey(d.key);
                  setSlotIso(null);
                }}
                style={[styles.day, active && styles.dayActive]}
              >
                <Text style={[styles.dayWeek, active && { color: colors.coral }]}>{d.weekday}</Text>
                <Text style={[styles.dayNum, active && { color: colors.white }]}>{d.dayNumber}</Text>
                <Text style={[styles.dayMonth, active && { color: 'rgba(255,255,255,0.7)' }]}>{d.month}</Text>
              </Pressable>
            );
          })}
        </ScrollView>

        <Text style={styles.group}>Horário</Text>
        <View style={styles.slots}>
          {slots.map((s) => {
            const active = s.iso === slotIso;
            return (
              <Pressable
                key={s.iso}
                testID={`slot-${s.label}`}
                disabled={s.disabled}
                accessibilityRole="button"
                accessibilityState={{ selected: active, disabled: s.disabled }}
                onPress={() => setSlotIso(s.iso)}
                style={[styles.slot, active && styles.slotActive, s.disabled && styles.slotDisabled]}
              >
                <Text style={[styles.slotText, active && { color: colors.white }, s.disabled && { color: colors.faint, textDecorationLine: 'line-through' }]}>{s.label}</Text>
              </Pressable>
            );
          })}
        </View>
        {errors.starts_at ? <Text style={styles.err}>{errors.starts_at}</Text> : null}

        <Text style={styles.group}>Seus dados</Text>
        <Field label="Nome" value={name} onChangeText={setName} error={errors.visitor_name} autoComplete="name" />
        <Field label="E-mail" value={email} onChangeText={setEmail} error={errors.visitor_email} autoCapitalize="none" keyboardType="email-address" />
        <Field label="Celular / WhatsApp" value={phone} onChangeText={(t) => setPhone(maskPhoneBR(t))} error={errors.visitor_phone} keyboardType="phone-pad" placeholder="(11) 90000-0000" />
        <Field label="Observações (opcional)" value={notes} onChangeText={setNotes} multiline placeholder="Ex.: vou com meu cônjuge, preciso de vaga para estacionar…" />
        {book.error ? <ErrorBox message={errorText(book.error)} /> : null}
      </ScrollView>
      <View style={[styles.bar, { paddingBottom: insets.bottom + space.md }]}>
        <View style={{ flex: 1 }}>
          <Text style={type.small}>{slotIso ? 'Visita em' : 'Selecione um horário'}</Text>
          <Text style={type.h3}>{slotIso ? formatVisitDate(slotIso) : '—'}</Text>
        </View>
        <Button variant="accent" title="Confirmar" loading={book.isPending} onPress={submit} testID="btn-confirm-visit" />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  listingCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, gap: 2, ...shadow },
  ref: { fontSize: 11, fontWeight: '800', color: colors.coral, letterSpacing: 0.8 },
  group: { ...type.label, marginTop: space.xl, marginBottom: space.md },
  day: { width: 62, paddingVertical: space.md, borderRadius: radius.md, backgroundColor: colors.white, borderWidth: 1.5, borderColor: colors.line, alignItems: 'center' },
  dayActive: { backgroundColor: colors.navy, borderColor: colors.navy },
  dayWeek: { fontSize: 12, fontWeight: '700', color: colors.muted, textTransform: 'capitalize' },
  dayNum: { fontSize: 22, fontWeight: '800', color: colors.text, marginVertical: 2 },
  dayMonth: { fontSize: 11, color: colors.muted },
  slots: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
  slot: { width: '23%', flexGrow: 1, maxWidth: '25%', paddingVertical: 12, borderRadius: radius.sm, backgroundColor: colors.white, borderWidth: 1.5, borderColor: colors.line, alignItems: 'center' },
  slotActive: { backgroundColor: colors.coral, borderColor: colors.coral },
  slotDisabled: { backgroundColor: colors.navySoft, borderColor: colors.navySoft },
  slotText: { fontSize: 15, fontWeight: '700', color: colors.text },
  err: { color: colors.danger, fontSize: 13, fontWeight: '600', marginTop: space.sm },
  bar: { position: 'absolute', left: 0, right: 0, bottom: 0, flexDirection: 'row', alignItems: 'center', gap: space.md, paddingHorizontal: space.lg, paddingTop: space.md, backgroundColor: colors.white, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.line },
  success: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: space.xxl, gap: space.sm, backgroundColor: colors.bg },
  successIcon: { width: 76, height: 76, borderRadius: 38, backgroundColor: colors.success, alignItems: 'center', justifyContent: 'center', marginBottom: space.md },
});
