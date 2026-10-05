import { StyleSheet, Text, View } from 'react-native';
import { APPLICATION_STATUS_LABEL, type ApplicationStatus } from '@regla/shared';
import { colors, space } from '@/theme';
import type { Tone } from './ui';
import { Icon } from './Icon';

export const STATUS_TONE: Record<ApplicationStatus, Tone> = {
  submitted: 'info',
  under_review: 'warning',
  docs_requested: 'accent',
  approved: 'success',
  rejected: 'danger',
  withdrawn: 'neutral',
};

interface Step {
  key: string;
  label: string;
  state: 'done' | 'current' | 'todo' | 'failed';
}

/** Linear timeline: Enviada → Em análise → (Documentos) → Decisão. */
export function timelineSteps(status: ApplicationStatus): Step[] {
  const final =
    status === 'approved' ? { label: 'Aprovada', state: 'done' as const }
    : status === 'rejected' ? { label: 'Recusada', state: 'failed' as const }
    : status === 'withdrawn' ? { label: 'Cancelada', state: 'failed' as const }
    : { label: 'Decisão', state: 'todo' as const };
  const reviewState: Step['state'] =
    status === 'under_review' ? 'current'
    : status === 'submitted' || status === 'withdrawn' ? 'todo'
    : 'done';
  const steps: Step[] = [
    { key: 'submitted', label: 'Enviada', state: 'done' },
    { key: 'under_review', label: 'Em análise', state: reviewState },
  ];
  if (status === 'docs_requested') steps.push({ key: 'docs', label: APPLICATION_STATUS_LABEL.docs_requested, state: 'current' });
  steps.push({ key: 'final', ...final });
  return steps;
}

export function StatusTimeline({ status }: { status: ApplicationStatus }) {
  const steps = timelineSteps(status);
  return (
    <View style={styles.wrap} testID="status-timeline">
      {steps.map((s, i) => {
        const color = s.state === 'done' ? colors.success : s.state === 'current' ? colors.coral : s.state === 'failed' ? colors.danger : colors.line;
        return (
          <View key={s.key} style={styles.step}>
            <View style={styles.dotRow}>
              <View style={[styles.line, { backgroundColor: i === 0 ? 'transparent' : steps[i - 1].state === 'done' ? colors.success : colors.line }]} />
              <View style={[styles.dot, { borderColor: color, backgroundColor: s.state === 'todo' ? colors.white : color }]}>
                {s.state === 'done' ? <Icon name="check" size={10} color={colors.white} /> : null}
                {s.state === 'failed' ? <Icon name="close" size={10} color={colors.white} /> : null}
              </View>
              <View style={[styles.line, { backgroundColor: i === steps.length - 1 ? 'transparent' : s.state === 'done' ? colors.success : colors.line }]} />
            </View>
            <Text style={[styles.label, s.state === 'todo' && { color: colors.faint }, s.state === 'current' && { color: colors.coralDark }]} numberOfLines={2}>
              {s.label}
            </Text>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', marginTop: space.md },
  step: { flex: 1, alignItems: 'center' },
  dotRow: { flexDirection: 'row', alignItems: 'center', alignSelf: 'stretch' },
  line: { flex: 1, height: 2 },
  dot: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 11, fontWeight: '700', color: colors.text, marginTop: 6, textAlign: 'center', paddingHorizontal: 2 },
});
