import { useState } from 'react';
import { Text, View } from 'react-native';
import type { ClientProfileInput } from '@regla/shared';
import { colors, space } from '@/theme';
import { validateProfileForm, type FieldErrors, type ProfileField, type ProfileFormValues } from '@/lib/forms';
import { ProfileForm } from './ProfileForm';
import { Button } from './ui';

interface Props {
  initial: ProfileFormValues;
  onSave: (input: ClientProfileInput) => void | Promise<unknown>;
  saving?: boolean;
  strict?: boolean;
  submitLabel?: string;
}

/** Self-contained "Meu cadastro" form: holds state, validates with the shared zod schema, calls onSave. */
export function ProfileEditor({ initial, onSave, saving, strict = false, submitLabel = 'Salvar cadastro' }: Props) {
  const [values, setValues] = useState<ProfileFormValues>(initial);
  const [errors, setErrors] = useState<FieldErrors<ProfileField>>({});

  const submit = () => {
    const res = validateProfileForm(values, strict);
    setErrors(res.errors);
    if (res.ok) void onSave(res.data);
  };

  const count = Object.keys(errors).length;

  return (
    <View>
      <ProfileForm values={values} onChange={setValues} errors={errors} />
      {count ? (
        <Text style={{ color: colors.danger, fontWeight: '600', marginTop: space.lg }} testID="form-error-summary">
          Corrija {count === 1 ? 'o campo destacado' : `os ${count} campos destacados`}.
        </Text>
      ) : null}
      <Button title={submitLabel} variant="accent" loading={saving} onPress={submit} style={{ marginTop: space.lg }} testID="btn-save-profile" />
    </View>
  );
}
