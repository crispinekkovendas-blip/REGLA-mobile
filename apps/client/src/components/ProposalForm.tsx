import { StyleSheet, Text, View } from 'react-native';
import { GUARANTEE_LABEL, affordability, formatPrice, type Currency, type GuaranteeType } from '@regla/shared';
import { colors, radius, space, type } from '@/theme';
import { maskDateBR, maskMoney, onlyDigits, type FieldErrors, type ProposalField, type ProposalFormValues } from '@/lib/forms';
import { Badge, Chip, Field, type Tone } from './ui';

interface Props {
  values: ProposalFormValues;
  onChange: (next: ProposalFormValues) => void;
  errors: FieldErrors<ProposalField>;
  askingPrice: number;
  currency: Currency;
  monthlyIncome: number | null;
}

const GUARANTEES = Object.keys(GUARANTEE_LABEL) as GuaranteeType[];

const AFFORD: Record<ReturnType<typeof affordability>, { tone: Tone; label: string; text: string }> = {
  ok: { tone: 'success', label: 'Cabe no orçamento', text: 'O aluguel fica em até 30% da sua renda — ótimo sinal para aprovação.' },
  tight: { tone: 'warning', label: 'Orçamento apertado', text: 'O aluguel compromete entre 30% e 40% da renda. Um fiador ou seguro fiança ajuda.' },
  over: { tone: 'danger', label: 'Acima do recomendado', text: 'O aluguel passa de 40% da renda informada. Considere incluir outra renda.' },
  unknown: { tone: 'neutral', label: 'Renda não informada', text: 'Informe sua renda no cadastro para vermos se o imóvel cabe no seu bolso.' },
};

export function AffordabilityBadge({ rent, income }: { rent: number; income: number | null }) {
  const a = affordability(rent, income);
  const info = AFFORD[a];
  const pct = income && income > 0 ? Math.round((rent / income) * 100) : null;
  return (
    <View style={styles.afford} testID={`affordability-${a}`}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: space.sm }}>
        <Badge label={info.label} tone={info.tone} />
        {pct != null ? <Text style={type.small}>{pct}% da renda</Text> : null}
      </View>
      <Text style={[type.small, { marginTop: 6 }]}>{info.text}</Text>
    </View>
  );
}

export function ProposalForm({ values, onChange, errors, askingPrice, currency, monthlyIncome }: Props) {
  const set = <K extends ProposalField>(k: K, v: ProposalFormValues[K]) => onChange({ ...values, [k]: v });
  const offered = Number(onlyDigits(values.offered_price) || '0');
  const diff = askingPrice > 0 && offered > 0 ? Math.round(((offered - askingPrice) / askingPrice) * 100) : 0;

  return (
    <View>
      <Text style={styles.label}>Você quer</Text>
      <View style={styles.segment}>
        {(['rent', 'buy'] as const).map((i) => (
          <Text
            key={i}
            accessibilityRole="button"
            accessibilityState={{ selected: values.intent === i }}
            onPress={() => onChange({ ...values, intent: i, guarantee_type: i === 'buy' ? null : values.guarantee_type })}
            style={[styles.segItem, values.intent === i && styles.segActive]}
          >
            {i === 'rent' ? 'Alugar' : 'Comprar'}
          </Text>
        ))}
      </View>

      <Field
        label={values.intent === 'rent' ? 'Valor do aluguel proposto (mensal)' : 'Valor da oferta'}
        prefix="R$"
        value={values.offered_price}
        onChangeText={(t) => set('offered_price', maskMoney(t))}
        keyboardType="number-pad"
        placeholder="0"
        error={errors.offered_price}
        hint={
          askingPrice > 0
            ? `Anunciado por ${formatPrice(askingPrice, currency)}${diff ? ` · sua proposta: ${diff > 0 ? '+' : ''}${diff}%` : ''}`
            : undefined
        }
        testID="field-offered_price"
      />

      {values.intent === 'rent' ? (
        <>
          <AffordabilityBadge rent={offered || askingPrice} income={monthlyIncome} />
          <Text style={[styles.label, { marginTop: space.lg }]}>Garantia</Text>
          <View style={styles.chips}>
            {GUARANTEES.map((g) => (
              <Chip key={g} label={GUARANTEE_LABEL[g]} active={values.guarantee_type === g} onPress={() => set('guarantee_type', g)} />
            ))}
          </View>
          {errors.guarantee_type ? <Text style={styles.error}>{errors.guarantee_type}</Text> : null}
          <View style={{ height: space.lg }} />
        </>
      ) : (
        <View style={[styles.afford, { marginBottom: space.lg }]}>
          <Text style={type.small}>Na compra, a análise de crédito e as condições de pagamento são combinadas com o corretor após o envio.</Text>
        </View>
      )}

      <Field
        label={values.intent === 'rent' ? 'Quando quer se mudar?' : 'Data desejada para escritura'}
        value={values.move_in_date}
        onChangeText={(t) => set('move_in_date', maskDateBR(t))}
        keyboardType="number-pad"
        placeholder="DD/MM/AAAA"
        error={errors.move_in_date}
        testID="field-move_in_date"
      />
      <Field
        label="Mensagem para o proprietário (opcional)"
        value={values.message}
        onChangeText={(t) => set('message', t)}
        multiline
        maxLength={2000}
        placeholder="Conte um pouco sobre você e o que achou do imóvel."
        error={errors.message}
        testID="field-message"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 13, fontWeight: '700', color: colors.text, marginBottom: space.sm },
  segment: { flexDirection: 'row', backgroundColor: colors.white, borderRadius: radius.md, borderWidth: 1.5, borderColor: colors.line, padding: 4, marginBottom: space.lg },
  segItem: { flex: 1, textAlign: 'center', paddingVertical: 11, borderRadius: radius.sm, fontWeight: '700', color: colors.muted, overflow: 'hidden' },
  segActive: { backgroundColor: colors.navy, color: colors.white },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
  error: { color: colors.danger, fontSize: 13, fontWeight: '600', marginTop: 6 },
  afford: { backgroundColor: colors.white, borderRadius: radius.md, borderWidth: 1.5, borderColor: colors.line, padding: space.md },
});
