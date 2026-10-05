import { useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { DOCUMENT_KIND_LABEL, type ClientDocument, type DocumentKind, type UploadableFile } from '@regla/shared';
import { colors, radius, space, type } from '@/theme';
import { MAX_UPLOAD_BYTES, formatBytes, pickDocumentFile, pickImageFile } from '@/lib/files';
import { errorText, useUploadDocument } from '@/lib/queries';
import { Icon } from './Icon';
import { Button } from './ui';

export const REQUIRED_DOCS: DocumentKind[] = ['rg_cnh', 'cpf', 'comprovante_renda', 'comprovante_residencia'];
export const OPTIONAL_DOCS: DocumentKind[] = ['extrato_bancario', 'imposto_renda', 'outro'];

const DOC_HINT: Partial<Record<DocumentKind, string>> = {
  rg_cnh: 'Frente e verso, legível',
  cpf: 'Dispensável se constar no RG/CNH',
  comprovante_renda: '3 últimos holerites ou extrato do pró-labore',
  comprovante_residencia: 'Conta de luz, água ou internet recente',
  extrato_bancario: 'Últimos 3 meses',
  imposto_renda: 'Declaração completa + recibo',
  outro: 'Qualquer documento complementar',
};

type Source = 'file' | 'gallery' | 'camera';

interface Props {
  documents: ClientDocument[];
  applicationId?: number | null;
  showOptional?: boolean;
}

export function DocumentChecklist({ documents, applicationId = null, showOptional = true }: Props) {
  const upload = useUploadDocument();
  const [open, setOpen] = useState<DocumentKind | null>(null);
  const [busy, setBusy] = useState<DocumentKind | null>(null);
  const [error, setError] = useState<{ kind: DocumentKind; message: string } | null>(null);

  const byKind = (k: DocumentKind) => documents.filter((d) => d.kind === k);

  async function handle(kind: DocumentKind, source: Source) {
    setError(null);
    try {
      let file: UploadableFile | null;
      if (source === 'file') file = await pickDocumentFile();
      else file = await pickImageFile(source === 'camera');
      if (!file) return;
      if (file.size > MAX_UPLOAD_BYTES) throw new Error(`Arquivo muito grande (${formatBytes(file.size)}). Limite: 10 MB.`);
      setBusy(kind);
      await upload.mutateAsync({ kind, file, applicationId });
      setOpen(null);
    } catch (e) {
      setError({ kind, message: errorText(e) });
    } finally {
      setBusy(null);
    }
  }

  const renderRow = (kind: DocumentKind, required: boolean) => {
    const files = byKind(kind);
    const done = files.length > 0;
    return (
      <View key={kind} style={styles.row} testID={`doc-row-${kind}`}>
        <Pressable style={styles.rowHead} onPress={() => setOpen(open === kind ? null : kind)} accessibilityRole="button" accessibilityLabel={`Enviar ${DOCUMENT_KIND_LABEL[kind]}`}>
          <View style={[styles.status, done ? styles.statusDone : required ? styles.statusTodo : styles.statusOpt]}>
            {done ? <Icon name="check" size={14} color={colors.white} /> : <Icon name="doc" size={16} color={required ? colors.coral : colors.faint} />}
          </View>
          <View style={{ flex: 1 }}>
            <Text style={type.h3}>
              {DOCUMENT_KIND_LABEL[kind]}
              {required ? <Text style={{ color: colors.coral }}> *</Text> : null}
            </Text>
            <Text style={type.small}>{done ? `${files.length} arquivo${files.length > 1 ? 's' : ''} enviado${files.length > 1 ? 's' : ''}` : DOC_HINT[kind]}</Text>
          </View>
          <View style={styles.addPill}>
            <Icon name={open === kind ? 'close' : 'plus'} size={14} color={colors.navy} />
          </View>
        </Pressable>
        {files.length ? (
          <View style={styles.files}>
            {files.map((f) => (
              <Text key={f.id} style={styles.fileName} numberOfLines={1}>
                {f.filename} · {formatBytes(f.size_bytes)}
              </Text>
            ))}
          </View>
        ) : null}
        {open === kind ? (
          <View style={styles.sources}>
            <Button small variant="outline" icon="doc" title="Arquivo" loading={busy === kind} onPress={() => handle(kind, 'file')} style={{ flex: 1 }} />
            <Button small variant="outline" icon="upload" title="Galeria" disabled={busy === kind} onPress={() => handle(kind, 'gallery')} style={{ flex: 1 }} />
            {Platform.OS !== 'web' ? (
              <Button small variant="outline" icon="plus" title="Câmera" disabled={busy === kind} onPress={() => handle(kind, 'camera')} style={{ flex: 1 }} />
            ) : null}
          </View>
        ) : null}
        {error?.kind === kind ? <Text style={styles.error}>{error.message}</Text> : null}
      </View>
    );
  };

  return (
    <View>
      {REQUIRED_DOCS.map((k) => renderRow(k, true))}
      {showOptional ? (
        <>
          <Text style={[type.label, { marginTop: space.lg, marginBottom: space.sm }]}>Opcionais — aceleram a análise</Text>
          {OPTIONAL_DOCS.map((k) => renderRow(k, false))}
        </>
      ) : null}
    </View>
  );
}

export const missingRequiredDocs = (docs: ClientDocument[]): DocumentKind[] =>
  REQUIRED_DOCS.filter((k) => k !== 'cpf' && !docs.some((d) => d.kind === k));

const styles = StyleSheet.create({
  row: { backgroundColor: colors.white, borderRadius: radius.md, borderWidth: 1.5, borderColor: colors.line, padding: space.md, marginBottom: space.sm },
  rowHead: { flexDirection: 'row', alignItems: 'center', gap: space.md },
  status: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  statusDone: { backgroundColor: colors.success },
  statusTodo: { backgroundColor: colors.coralSoft },
  statusOpt: { backgroundColor: colors.navySoft },
  addPill: { width: 32, height: 32, borderRadius: 16, backgroundColor: colors.navySoft, alignItems: 'center', justifyContent: 'center' },
  files: { marginTop: space.sm, marginLeft: 48, gap: 2 },
  fileName: { fontSize: 12, color: colors.muted },
  sources: { flexDirection: 'row', gap: space.sm, marginTop: space.md },
  error: { color: colors.danger, fontSize: 13, fontWeight: '600', marginTop: space.sm },
});
