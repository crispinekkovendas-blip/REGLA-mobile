import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import type { UploadableFile } from '@regla/shared';

export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // 10 MB

async function readBody(uri: string, webFile?: Blob): Promise<ArrayBuffer | Blob> {
  if (webFile) return webFile;
  const res = await fetch(uri);
  // RN's fetch supports arrayBuffer(); Supabase storage accepts both.
  return typeof res.arrayBuffer === 'function' ? res.arrayBuffer() : res.blob();
}

function guessMime(name: string): string {
  const ext = name.split('.').pop()?.toLowerCase();
  if (ext === 'pdf') return 'application/pdf';
  if (ext === 'png') return 'image/png';
  if (ext === 'heic') return 'image/heic';
  if (ext === 'webp') return 'image/webp';
  return 'image/jpeg';
}

/** Pick a PDF or image from the file system. Returns null when cancelled. */
export async function pickDocumentFile(): Promise<UploadableFile | null> {
  const res = await DocumentPicker.getDocumentAsync({
    type: ['application/pdf', 'image/*'],
    copyToCacheDirectory: true,
    multiple: false,
  });
  if (res.canceled || !res.assets?.length) return null;
  const a = res.assets[0];
  const body = await readBody(a.uri, a.file);
  const size = a.size ?? (body instanceof ArrayBuffer ? body.byteLength : (body as Blob).size);
  return { uri: a.uri, name: a.name, mimeType: a.mimeType ?? guessMime(a.name), size, body };
}

/** Take a photo of a document (or pick from gallery when camera = false). */
export async function pickImageFile(camera: boolean): Promise<UploadableFile | null> {
  const perm = camera
    ? await ImagePicker.requestCameraPermissionsAsync()
    : await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!perm.granted) throw new Error(camera ? 'Permissão da câmera negada.' : 'Permissão da galeria negada.');
  const opts: ImagePicker.ImagePickerOptions = { mediaTypes: ['images'], quality: 0.7, allowsEditing: false };
  const res = camera ? await ImagePicker.launchCameraAsync(opts) : await ImagePicker.launchImageLibraryAsync(opts);
  if (res.canceled || !res.assets?.length) return null;
  const a = res.assets[0];
  const name = a.fileName ?? `foto-${Date.now()}.jpg`;
  const body = await readBody(a.uri, a.file);
  const size = a.fileSize ?? (body instanceof ArrayBuffer ? body.byteLength : (body as Blob).size);
  return { uri: a.uri, name, mimeType: a.mimeType ?? guessMime(name), size, body };
}

export function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`;
  return `${(n / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
}
