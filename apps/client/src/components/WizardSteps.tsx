import { StyleSheet, Text, View } from 'react-native';
import { colors, space } from '@/theme';

export const WIZARD_STEPS = ['Cadastro', 'Proposta', 'Documentos', 'Revisão'] as const;

export function WizardSteps({ current }: { current: number }) {
  return (
    <View style={styles.wrap} accessibilityLabel={`Etapa ${current + 1} de ${WIZARD_STEPS.length}`} testID="wizard-steps">
      {WIZARD_STEPS.map((label, i) => {
        const done = i < current;
        const active = i === current;
        return (
          <View key={label} style={styles.step}>
            <View style={[styles.bar, (done || active) && { backgroundColor: active ? colors.coral : colors.navy }]} />
            <Text style={[styles.label, active && { color: colors.coral }, done && { color: colors.navy }]}>
              {i + 1}. {label}
            </Text>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', gap: 6, paddingHorizontal: space.lg, paddingTop: space.sm, paddingBottom: space.md, backgroundColor: colors.bg },
  step: { flex: 1, gap: 6 },
  bar: { height: 4, borderRadius: 2, backgroundColor: colors.line },
  label: { fontSize: 11, fontWeight: '700', color: colors.faint },
});
