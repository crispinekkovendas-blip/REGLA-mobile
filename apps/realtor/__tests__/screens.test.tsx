import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';

const mockSignIn = jest.fn((_e: string, _p: string) => Promise.resolve());
jest.mock('../src/lib/auth', () => ({
  useAuth: () => ({ signIn: mockSignIn }),
}));

import LoginScreen from '../src/app/login';
import { ConfigureEnvScreen, RestrictedScreen } from '../src/components/GateScreens';

describe('gate screens', () => {
  it('login validates and calls signIn', async () => {
    await render(<LoginScreen />);
    await fireEvent.press(screen.getByTestId('login-submit'));
    expect(screen.getByText('Informe e-mail e senha.')).toBeTruthy();
    expect(mockSignIn).not.toHaveBeenCalled();

    await fireEvent.changeText(screen.getByTestId('login-email'), 'corretor@imoveisregla.com.br');
    await fireEvent.changeText(screen.getByTestId('login-password'), 'test-pass');
    await fireEvent.press(screen.getByTestId('login-submit'));
    await waitFor(() => expect(mockSignIn).toHaveBeenCalledWith('corretor@imoveisregla.com.br', 'test-pass'));
  });

  it('restricted screen shows the message and signs out', async () => {
    const onSignOut = jest.fn();
    await render(<RestrictedScreen email="cliente@x.com" onSignOut={onSignOut} />);
    expect(screen.getByText('Acesso restrito a corretores REGLA')).toBeTruthy();
    await fireEvent.press(screen.getByTestId('restricted-signout'));
    expect(onSignOut).toHaveBeenCalled();
  });

  it('configure-env screen renders', async () => {
    await render(<ConfigureEnvScreen />);
    expect(screen.getByText('Configure o .env')).toBeTruthy();
  });
});
