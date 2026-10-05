import { fireEvent, render, screen } from '@testing-library/react-native';
import { StatGrid, StatTile } from '../src/components/StatTile';

describe('StatTile', () => {
  it('renders the value and label', async () => {
    await render(<StatTile label="Novos leads" value={7} testID="t" />);
    expect(screen.getByText('7')).toBeTruthy();
    expect(screen.getByText('Novos leads')).toBeTruthy();
  });

  it('renders zero (not a placeholder) when the value is 0', async () => {
    await render(<StatTile label="Visitas hoje" value={0} testID="t" />);
    expect(screen.getByTestId('t-value').props.children).toBe('0');
  });

  it('shows a dash while loading', async () => {
    await render(<StatTile label="Imóveis ativos" value={undefined} testID="t" />);
    expect(screen.getByTestId('t-value').props.children).toBe('—');
  });
});

describe('StatGrid (dashboard tiles)', () => {
  const stats = { newLeads: 12, pendingApplications: 3, visitsToday: 5, liveListings: 39 };

  it('renders every dashboard number with its label', async () => {
    await render(<StatGrid stats={stats} />);
    expect(screen.getByTestId('stat-newLeads-value').props.children).toBe('12');
    expect(screen.getByTestId('stat-pendingApplications-value').props.children).toBe('3');
    expect(screen.getByTestId('stat-visitsToday-value').props.children).toBe('5');
    expect(screen.getByTestId('stat-liveListings-value').props.children).toBe('39');
    for (const label of ['Novos leads', 'Propostas pendentes', 'Visitas hoje', 'Imóveis ativos']) {
      expect(screen.getByText(label)).toBeTruthy();
    }
  });

  it('reports which tile was pressed', async () => {
    const onPress = jest.fn();
    await render(<StatGrid stats={stats} onPress={onPress} />);
    await fireEvent.press(screen.getByTestId('stat-pendingApplications'));
    expect(onPress).toHaveBeenCalledWith('pendingApplications');
  });
});
