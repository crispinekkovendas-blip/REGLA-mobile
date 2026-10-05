import { useMemo, useState } from 'react';
import { Text } from 'react-native';
import { router } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { APPLICATION_STATUS_LABEL, fetchApplicationsForReview, type ApplicationStatus } from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { ApplicationRow } from '../../components/rows';
import { Chip, ChipRow, EmptyState, ErrorState, Loading, Screen } from '../../components/ui';
import { APPLICATION_STATUSES, countBy } from '../../utils/pipeline';
import { type } from '../../theme';

type Filter = ApplicationStatus | 'all' | 'pending';

export default function ApplicationsScreen() {
  const [filter, setFilter] = useState<Filter>('pending');
  const q = useQuery({ queryKey: qk.applications, queryFn: () => fetchApplicationsForReview(getSupabase()) });
  const apps = q.data ?? [];
  const counts = useMemo(() => countBy(apps, (a) => a.status, APPLICATION_STATUSES), [apps]);
  const pending = counts.submitted + counts.under_review;
  const visible = apps.filter((a) =>
    filter === 'all' || (filter === 'pending' ? a.status === 'submitted' || a.status === 'under_review' : a.status === filter));

  return (
    <Screen refreshing={q.isRefetching} onRefresh={() => q.refetch()}>
      <ChipRow>
        <Chip label="Pendentes" count={pending} active={filter === 'pending'} onPress={() => setFilter('pending')} testID="app-filter-pending" />
        <Chip label="Todas" count={apps.length} active={filter === 'all'} onPress={() => setFilter('all')} testID="app-filter-all" />
        {APPLICATION_STATUSES.map((s) => (
          <Chip key={s} label={APPLICATION_STATUS_LABEL[s]} count={counts[s]} active={filter === s} onPress={() => setFilter(s)} testID={`app-filter-${s}`} />
        ))}
      </ChipRow>
      {q.isLoading ? <Loading /> : q.error ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : visible.length ? (
        <>
          <Text style={type.small}>{visible.length} {visible.length === 1 ? 'proposta' : 'propostas'}</Text>
          {visible.map((a) => (
            <ApplicationRow key={a.id} app={a} onPress={() => router.push({ pathname: '/application/[id]', params: { id: String(a.id) } })} />
          ))}
        </>
      ) : <EmptyState title="Nenhuma proposta neste filtro" hint="Propostas enviadas pelos clientes no app REGLA chegam aqui em tempo real." />}
    </Screen>
  );
}
