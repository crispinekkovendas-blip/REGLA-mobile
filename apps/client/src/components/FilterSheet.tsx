import type { ReactNode } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, radius, space, type } from '@/theme';
import { Icon } from './Icon';

export interface Option<T> {
  value: T;
  label: string;
}

interface Props<T> {
  visible: boolean;
  title: string;
  options: Option<T>[];
  selected: T | undefined;
  onSelect: (value: T | undefined) => void;
  onClose: () => void;
  footer?: ReactNode;
}

/** Bottom sheet with a single-choice list. Selecting the active option clears it. */
export function FilterSheet<T extends string | number>({ visible, title, options, selected, onSelect, onClose, footer }: Props<T>) {
  const insets = useSafeAreaInsets();
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel="Fechar" />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + space.lg }]}>
        <View style={styles.grabber} />
        <View style={styles.head}>
          <Text style={type.h2}>{title}</Text>
          <Pressable onPress={onClose} hitSlop={10} accessibilityLabel="Fechar filtro">
            <Icon name="close" size={20} color={colors.muted} />
          </Pressable>
        </View>
        <ScrollView style={{ maxHeight: 380 }}>
          <Pressable style={styles.opt} onPress={() => { onSelect(undefined); onClose(); }}>
            <Text style={[styles.optText, selected === undefined && styles.optActive]}>Todos</Text>
            {selected === undefined ? <Icon name="check" size={16} color={colors.coral} /> : null}
          </Pressable>
          {options.map((o) => {
            const active = o.value === selected;
            return (
              <Pressable key={String(o.value)} style={styles.opt} onPress={() => { onSelect(active ? undefined : o.value); onClose(); }}>
                <Text style={[styles.optText, active && styles.optActive]}>{o.label}</Text>
                {active ? <Icon name="check" size={16} color={colors.coral} /> : null}
              </Pressable>
            );
          })}
        </ScrollView>
        {footer}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(7,22,41,0.45)' },
  sheet: { backgroundColor: colors.white, borderTopLeftRadius: radius.xl, borderTopRightRadius: radius.xl, paddingHorizontal: space.xl, paddingTop: space.sm },
  grabber: { alignSelf: 'center', width: 40, height: 5, borderRadius: 3, backgroundColor: colors.line, marginBottom: space.md },
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: space.sm },
  opt: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 15, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  optText: { fontSize: 16, color: colors.text },
  optActive: { fontWeight: '800', color: colors.navy },
});
