import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ListingWithPhotos } from '@regla/shared';
import { ListingCard } from '@/components/ListingCard';

jest.mock('@/lib/supabase', () => ({
  isConfigured: false,
  SUPABASE_URL: 'https://test.supabase.co',
  supabase: {},
  listingPhotoUrl: (p: string) => `https://test.supabase.co/storage/v1/object/public/listing-photos/${p}`,
}));

const base: ListingWithPhotos = {
  id: 42,
  slug: 'apto-pinheiros',
  title: 'Apartamento iluminado em Pinheiros',
  city: 'São Paulo',
  country: 'BR',
  neighborhood: 'Pinheiros',
  type: 'apartment',
  price: 4500,
  currency: 'BRL',
  beds: 2,
  baths: 1,
  area_m2: 68,
  area_ft2: null,
  tags: ['aluguel'],
  palette: '',
  shape: '',
  summary: '',
  description: '',
  status: 'live',
  featured: false,
  agent_id: null,
  created_at: '2026-10-01T00:00:00Z',
  updated_at: '2026-10-01T00:00:00Z',
  listing_photos: [],
};

describe('ListingCard', () => {
  it('renders price, reference code, location and specs', async () => {
    await render(<ListingCard listing={base} />);
    expect(screen.getByTestId('listing-price').props.children).toMatch(/R\$\s?4\.500\/mês/);
    expect(screen.getByText('RG-0042')).toBeTruthy();
    expect(screen.getByText('Pinheiros · São Paulo')).toBeTruthy();
    expect(screen.getByText('2 quartos')).toBeTruthy();
    expect(screen.getByText('1 banheiro')).toBeTruthy();
    expect(screen.getByText('68 m²')).toBeTruthy();
    expect(screen.getByText('Aluguel')).toBeTruthy();
  });

  it('shows a placeholder block when there is no photo', async () => {
    await render(<ListingCard listing={base} />);
    expect(screen.getByTestId('listing-placeholder')).toBeTruthy();
  });

  it('labels sales without the /mês suffix', async () => {
    await render(<ListingCard listing={{ ...base, id: 7, price: 1_850_000, tags: [] }} />);
    expect(screen.getByTestId('listing-price').props.children).toMatch(/R\$\s?1\.850\.000$/);
    expect(screen.getByText('RG-0007')).toBeTruthy();
    expect(screen.getByText('Venda')).toBeTruthy();
  });

  it('calls the heart toggle and reflects favorite state', async () => {
    const onToggle = jest.fn();
    const onPress = jest.fn();
    await render(<ListingCard listing={base} isFavorite onToggleFavorite={onToggle} onPress={onPress} />);
    expect(screen.getByLabelText('Remover dos favoritos')).toBeTruthy();
    await fireEvent.press(screen.getByTestId('fav-42'));
    expect(onToggle).toHaveBeenCalledWith(expect.objectContaining({ id: 42 }));
    await fireEvent.press(screen.getByTestId('listing-card-42'));
    expect(onPress).toHaveBeenCalled();
  });
});
