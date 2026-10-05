import { Linking, StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';
import {
  APPLICATION_STATUS_LABEL, SHOWING_STATUS_LABEL, STAGE_LABEL, formatPrice, listingRef, photoUrl,
  type ApplicationForReview, type InquiryWithListing, type ListingWithPhotos, type ShowingStatus,
  type ShowingWithListing,
} from '@regla/shared';
import { colors, radius, space, type } from '../theme';
import {
  APPLICATION_STATUS_TONE, INTENT_LABEL, LISTING_STATUS_LABEL, LISTING_STATUS_TONE, LISTING_TYPE_LABEL,
  PRIORITY_LABEL, PRIORITY_TONE, SHOWING_STATUS_TONE, STAGE_TONE,
} from '../utils/labels';
import { relativeTime, timeLabel, dayLabel } from '../utils/agenda';
import { SUPABASE_URL } from '../lib/supabase';
import { Avatar, Button, Card, Pill, Row } from './ui';

// ─── Lead ─────────────────────────────────────────────────────────────

export function LeadRow({ lead, onPress }: { lead: InquiryWithListing; onPress: () => void }) {
  return (
    <Card onPress={onPress} style={styles.row} testID={`lead-${lead.id}`}>
      <View style={styles.rowTop}>
        <View style={styles.dotSlot}>
          {!lead.read ? <View style={styles.unreadDot} testID={`lead-${lead.id}-unread`} /> : null}
        </View>
        <View style={{ flex: 1, gap: 2 }}>
          <Row style={{ justifyContent: 'space-between' }}>
            <Text style={[styles.name, !lead.read && { fontWeight: '800' }]} numberOfLines={1}>{lead.name}</Text>
            <Text style={type.small}>{relativeTime(lead.last_activity_at)}</Text>
          </Row>
          <Text style={styles.sub} numberOfLines={1}>
            {lead.listings ? `${listingRef(lead.listings.id)} · ${lead.listings.title}` : lead.email}
          </Text>
          <Text style={styles.msg} numberOfLines={2}>{lead.message}</Text>
          <Row style={{ marginTop: 6 }} gap={6}>
            <Pill label={STAGE_LABEL[lead.stage]} tone={STAGE_TONE[lead.stage]} />
            <Pill label={`Prioridade ${PRIORITY_LABEL[lead.priority].toLowerCase()}`} tone={PRIORITY_TONE[lead.priority]} dot={false} />
            {lead.intent ? <Text style={type.small}>{INTENT_LABEL[lead.intent] ?? lead.intent}</Text> : null}
          </Row>
        </View>
      </View>
    </Card>
  );
}

// ─── Application ──────────────────────────────────────────────────────

export function ApplicationRow({ app, onPress }: { app: ApplicationForReview; onPress: () => void }) {
  const name = app.client_profiles?.full_name ?? 'Cliente sem cadastro';
  const listing = app.listings;
  return (
    <Card onPress={onPress} style={styles.row} testID={`application-${app.id}`}>
      <View style={styles.rowTop}>
        <Avatar name={name} size={38} />
        <View style={{ flex: 1, gap: 2 }}>
          <Row style={{ justifyContent: 'space-between' }}>
            <Text style={styles.name} numberOfLines={1}>{name}</Text>
            <Text style={type.small}>{relativeTime(app.created_at)}</Text>
          </Row>
          <Text style={styles.sub} numberOfLines={1}>
            {listing ? `${listingRef(listing.id)} · ${listing.title}` : `Imóvel #${app.listing_id}`}
          </Text>
          <Row style={{ marginTop: 6, justifyContent: 'space-between' }}>
            <Pill label={APPLICATION_STATUS_LABEL[app.status]} tone={APPLICATION_STATUS_TONE[app.status]} />
            <Text style={styles.money}>
              {INTENT_LABEL[app.intent]} · {formatPrice(app.offered_price, listing?.currency ?? 'BRL')}
            </Text>
          </Row>
        </View>
      </View>
    </Card>
  );
}

// ─── Showing (agenda) ─────────────────────────────────────────────────

const ACTIONS: Record<ShowingStatus, { status: ShowingStatus; label: string; variant: 'ok' | 'secondary' | 'danger' | 'primary' }[]> = {
  scheduled: [
    { status: 'confirmed', label: 'Confirmar', variant: 'primary' },
    { status: 'attended', label: 'Realizada', variant: 'secondary' },
    { status: 'no_show', label: 'Não compareceu', variant: 'secondary' },
    { status: 'cancelled', label: 'Cancelar', variant: 'danger' },
  ],
  confirmed: [
    { status: 'attended', label: 'Realizada', variant: 'ok' },
    { status: 'no_show', label: 'Não compareceu', variant: 'secondary' },
    { status: 'cancelled', label: 'Cancelar', variant: 'danger' },
  ],
  attended: [],
  no_show: [{ status: 'scheduled', label: 'Reagendar', variant: 'secondary' }],
  cancelled: [{ status: 'scheduled', label: 'Reabrir', variant: 'secondary' }],
};

export function showingActions(status: ShowingStatus) {
  return ACTIONS[status];
}

export function ShowingItem({ showing, onStatus, busy, compact, showDay }: {
  showing: ShowingWithListing;
  onStatus?: (s: ShowingStatus) => void;
  busy?: boolean;
  compact?: boolean;
  showDay?: boolean;
}) {
  const end = new Date(Date.parse(showing.starts_at) + showing.duration_minutes * 60_000).toISOString();
  const done = ['attended', 'no_show', 'cancelled'].includes(showing.status);
  const phone = showing.visitor_phone;
  return (
    <Card style={[styles.showing, done && { opacity: 0.72 }]} testID={`showing-${showing.id}`}>
      <View style={styles.timeCol}>
        {showDay ? <Text style={styles.timeDay}>{dayLabel(new Date(showing.starts_at))}</Text> : null}
        <Text style={styles.time}>{timeLabel(showing.starts_at)}</Text>
        <Text style={styles.timeEnd}>{timeLabel(end)}</Text>
      </View>
      <View style={{ flex: 1, gap: 3 }}>
        <Row style={{ justifyContent: 'space-between' }}>
          <Text style={styles.name} numberOfLines={1}>{showing.visitor_name ?? 'Visitante'}</Text>
          <Pill label={SHOWING_STATUS_LABEL[showing.status]} tone={SHOWING_STATUS_TONE[showing.status]} />
        </Row>
        <Text style={styles.sub} numberOfLines={1}>
          {showing.listings ? `${listingRef(showing.listings.id)} · ${showing.listings.title}` : `Imóvel #${showing.listing_id}`}
        </Text>
        {showing.listings ? <Text style={type.small} numberOfLines={1}>{showing.listings.neighborhood}, {showing.listings.city}</Text> : null}
        {!compact && showing.notes ? <Text style={styles.msg} numberOfLines={2}>“{showing.notes}”</Text> : null}
        {!compact && (onStatus || phone) ? (
          <View style={styles.actions}>
            {onStatus && ACTIONS[showing.status].map((a) => (
              <Button
                key={a.status}
                small
                label={a.label}
                variant={a.variant}
                disabled={busy}
                onPress={() => onStatus(a.status)}
                testID={`showing-${showing.id}-${a.status}`}
              />
            ))}
            {phone ? <Button small variant="ghost" label="Ligar" onPress={() => Linking.openURL(`tel:${phone.replace(/[^\d+]/g, '')}`)} /> : null}
          </View>
        ) : null}
      </View>
    </Card>
  );
}

// ─── Listing ──────────────────────────────────────────────────────────

export function ListingThumb({ listing, size = 64 }: { listing: ListingWithPhotos; size?: number }) {
  const photo = listing.listing_photos[0];
  if (photo && SUPABASE_URL) {
    return (
      <Image
        source={{ uri: photoUrl(SUPABASE_URL, photo.storage_path) }}
        style={{ width: size, height: size, borderRadius: radius.md, backgroundColor: colors.navySoft }}
        contentFit="cover"
        accessibilityLabel={photo.alt_text ?? listing.title}
      />
    );
  }
  return (
    <View style={{ width: size, height: size, borderRadius: radius.md, backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center' }}>
      <Text style={{ color: colors.coral, fontWeight: '900', fontSize: size * 0.28 }}>R</Text>
    </View>
  );
}

export function ListingRow({ listing, onPress }: { listing: ListingWithPhotos; onPress: () => void }) {
  return (
    <Card onPress={onPress} style={styles.row} testID={`listing-${listing.id}`}>
      <View style={styles.rowTop}>
        <ListingThumb listing={listing} />
        <View style={{ flex: 1, gap: 2 }}>
          <Row style={{ justifyContent: 'space-between' }}>
            <Text style={type.mono}>{listingRef(listing.id)}</Text>
            <Pill label={LISTING_STATUS_LABEL[listing.status]} tone={LISTING_STATUS_TONE[listing.status]} />
          </Row>
          <Text style={styles.name} numberOfLines={1}>{listing.title}</Text>
          <Text style={styles.sub} numberOfLines={1}>
            {LISTING_TYPE_LABEL[listing.type]} · {listing.neighborhood}, {listing.city}
          </Text>
          <Text style={styles.money}>
            {formatPrice(listing.price, listing.currency)} · {listing.beds} qto · {listing.area_m2} m²
          </Text>
        </View>
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  row: { padding: space.md },
  rowTop: { flexDirection: 'row', gap: space.md, alignItems: 'flex-start' },
  dotSlot: { width: 8, paddingTop: 6 },
  unreadDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.coral },
  name: { ...type.h2, flexShrink: 1 },
  sub: { fontSize: 13, color: colors.muted, fontWeight: '500' },
  msg: { fontSize: 13, color: colors.text, lineHeight: 18, marginTop: 2 },
  money: { fontSize: 13, fontWeight: '700', color: colors.navy, ...type.num },
  showing: { flexDirection: 'row', gap: space.md, padding: space.md },
  timeCol: { width: 54, borderRightWidth: 2, borderRightColor: colors.coral, paddingRight: space.sm },
  timeDay: { fontSize: 10, fontWeight: '700', color: colors.coral, textTransform: 'uppercase' },
  time: { fontSize: 17, fontWeight: '800', color: colors.navy, ...type.num },
  timeEnd: { fontSize: 12, color: colors.muted, ...type.num },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: space.sm },
});
