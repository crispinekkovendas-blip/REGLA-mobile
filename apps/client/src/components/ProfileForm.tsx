import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { EMPLOYMENT_LABEL, maskCpf, maskPhoneBR, type EmploymentType } from '@regla/shared';
import { colors, radius, space, type } from '@/theme';
import { maskDateBR, maskMoney, type FieldErrors, type ProfileField, type ProfileFormValues } from '@/lib/forms';
import { Chip, Field } from './ui';

interface Props {
  values: ProfileFormValues;
  onChange: (next: ProfileFormValues) => void;
  errors: FieldErrors<ProfileField>;
}

const EMPLOYMENT = Object.keys(EMPLOYMENT_LABEL) as EmploymentType[];

export function ProfileForm({ values, onChange, errors }: Props) {
  const set = <K extends ProfileField>(k: K, v: ProfileFormValues[K]) => onChange({ ...values, [k]: v });

  return (
    <View>
      <Text style={styles.group}>Dados pessoais</Text>
      <Field
        label="Nome completo"
        value={values.full_name}
        onChangeText={(t) => set('full_name', t)}
        error={errors.full_name}
        autoComplete="name"
        placeholder="Como no seu documento"
        testID="field-full_name"
      />
      <Field
        label="E-mail"
        value={values.email}
        onChangeText={(t) => set('email', t)}
        error={errors.email}
        autoCapitalize="none"
        keyboardType="email-address"
        autoComplete="email"
        placeholder="voce@email.com"
        testID="field-email"
      />
      <View style={styles.row}>
        <View style={{ flex: 1 }}>
          <Field
            label="Celular"
            value={values.phone}
            onChangeText={(t) => set('phone', maskPhoneBR(t))}
            error={errors.phone}
            keyboardType="phone-pad"
            placeholder="(11) 90000-0000"
            testID="field-phone"
          />
        </View>
      </View>
      <View style={styles.row}>
        <View style={{ flex: 1.2 }}>
          <Field
            label="CPF"
            value={values.cpf}
            onChangeText={(t) => set('cpf', maskCpf(t))}
            error={errors.cpf}
            keyboardType="number-pad"
            placeholder="000.000.000-00"
            testID="field-cpf"
          />
        </View>
        <View style={{ flex: 1 }}>
          <Field
            label="Nascimento"
            value={values.birth_date}
            onChangeText={(t) => set('birth_date', maskDateBR(t))}
            error={errors.birth_date}
            keyboardType="number-pad"
            placeholder="DD/MM/AAAA"
            testID="field-birth_date"
          />
        </View>
      </View>

      <Text style={styles.group}>Renda e ocupação</Text>
      <Text style={styles.label}>Vínculo de trabalho</Text>
      <View style={styles.chips}>
        {EMPLOYMENT.map((e) => (
          <Chip key={e} label={EMPLOYMENT_LABEL[e]} active={values.employment_type === e} onPress={() => set('employment_type', values.employment_type === e ? null : e)} />
        ))}
      </View>
      {errors.employment_type ? <Text style={styles.error}>{errors.employment_type}</Text> : null}
      <View style={{ height: space.lg }} />
      <Field
        label="Profissão"
        value={values.occupation}
        onChangeText={(t) => set('occupation', t)}
        error={errors.occupation}
        placeholder="Ex.: Analista de sistemas"
        testID="field-occupation"
      />
      <Field
        label="Renda mensal bruta"
        prefix="R$"
        value={values.monthly_income}
        onChangeText={(t) => set('monthly_income', maskMoney(t))}
        error={errors.monthly_income}
        keyboardType="number-pad"
        placeholder="0"
        hint="Some a renda de todos que vão morar e comprovar renda."
        testID="field-monthly_income"
      />

      <Text style={styles.group}>Moradia</Text>
      <View style={styles.inlineRow}>
        <View style={{ flex: 1 }}>
          <Text style={type.h3}>Moradores</Text>
          <Text style={type.small}>Incluindo você</Text>
        </View>
        <View style={styles.stepper}>
          <Pressable accessibilityLabel="Diminuir moradores" style={styles.stepBtn} onPress={() => set('residents', Math.max(1, values.residents - 1))}>
            <Text style={styles.stepTxt}>−</Text>
          </Pressable>
          <Text style={styles.stepVal} testID="residents-value">
            {values.residents}
          </Text>
          <Pressable accessibilityLabel="Aumentar moradores" style={styles.stepBtn} onPress={() => set('residents', Math.min(20, values.residents + 1))}>
            <Text style={styles.stepTxt}>+</Text>
          </Pressable>
        </View>
      </View>
      {errors.residents ? <Text style={styles.error}>{errors.residents}</Text> : null}
      <View style={[styles.inlineRow, { marginTop: space.md }]}>
        <View style={{ flex: 1 }}>
          <Text style={type.h3}>Tenho pets</Text>
          <Text style={type.small}>Cães, gatos ou outros animais</Text>
        </View>
        <Switch
          accessibilityLabel="Tenho pets"
          value={values.has_pets}
          onValueChange={(v) => set('has_pets', v)}
          trackColor={{ true: colors.coral, false: colors.line }}
          thumbColor={colors.white}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  group: { ...type.label, marginTop: space.lg, marginBottom: space.md, color: colors.navy },
  label: { fontSize: 13, fontWeight: '700', color: colors.text, marginBottom: space.sm },
  row: { flexDirection: 'row', gap: space.md },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
  error: { color: colors.danger, fontSize: 13, fontWeight: '600', marginTop: 6 },
  inlineRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.white, borderRadius: radius.md, borderWidth: 1.5, borderColor: colors.line, padding: space.md },
  stepper: { flexDirection: 'row', alignItems: 'center', gap: space.md },
  stepBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: colors.navySoft, alignItems: 'center', justifyContent: 'center' },
  stepTxt: { fontSize: 20, fontWeight: '700', color: colors.navy, lineHeight: 22 },
  stepVal: { fontSize: 17, fontWeight: '800', color: colors.text, minWidth: 20, textAlign: 'center' },
});
