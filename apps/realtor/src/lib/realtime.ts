import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { supabase } from './supabase';
import { qk } from './queryClient';

/**
 * Subscribe to new applications ("propostas") and inquiries ("leads") and
 * refresh the affected queries. Returns whether the channel is live.
 */
export function useRealtimeInbox(enabled: boolean): boolean {
  const qc = useQueryClient();
  const [live, setLive] = useState(false);

  useEffect(() => {
    const sb = supabase;
    if (!enabled || !sb) return;
    const channel = sb
      .channel('realtor-inbox')
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'applications' }, () => {
        qc.invalidateQueries({ queryKey: qk.stats });
        qc.invalidateQueries({ queryKey: qk.applications });
      })
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'inquiries' }, () => {
        qc.invalidateQueries({ queryKey: qk.stats });
        qc.invalidateQueries({ queryKey: qk.leads });
      })
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'showings' }, () => {
        qc.invalidateQueries({ queryKey: qk.stats });
        qc.invalidateQueries({ queryKey: qk.agendaAll });
      })
      .subscribe((status) => setLive(status === 'SUBSCRIBED'));
    return () => {
      setLive(false);
      sb.removeChannel(channel);
    };
  }, [enabled, qc]);

  return live;
}
