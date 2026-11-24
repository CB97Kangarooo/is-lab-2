import { useEffect, useRef } from 'react';
import { useRealtime } from './RealtimeProvider';
import type { ChangeEntity, RealtimeEvent } from './socket';

interface Options {
  entities?: readonly ChangeEntity[];
  windowMs?: number;
}

const DEFAULT_WINDOW_MS = 200;

export function useChangeSignal(handler: (events: RealtimeEvent[]) => void, options: Options = {}): void {
  const { subscribe } = useRealtime();
  const handlerRef = useRef(handler);
  handlerRef.current = handler;
  const windowMs = options.windowMs ?? DEFAULT_WINDOW_MS;
  const entitiesKey = options.entities?.join(',') ?? '';

  useEffect(() => {
    const entities = entitiesKey ? entitiesKey.split(',') : null;
    let batch: RealtimeEvent[] = [];
    let timer: ReturnType<typeof setTimeout> | undefined;

    const unsubscribe = subscribe((event) => {
      if (event.type === 'change' && entities && !entities.includes(event.entity)) return;
      batch.push(event);
      if (timer === undefined) {
        timer = setTimeout(() => {
          const events = batch;
          batch = [];
          timer = undefined;
          handlerRef.current(events);
        }, windowMs);
      }
    });
    return () => {
      unsubscribe();
      clearTimeout(timer);
    };
  }, [subscribe, entitiesKey, windowMs]);
}

export function touchesHero(events: RealtimeEvent[], id: number): { deleted: boolean; maybeChanged: boolean } {
  let deleted = false;
  let maybeChanged = false;
  for (const event of events) {
    if (event.type === 'resync') {
      maybeChanged = true;
    } else if (event.entity === 'human') {
      if (event.change === 'bulk') maybeChanged = true;
      if (event.id === id && event.change === 'deleted') deleted = true;
      if (event.id === id && event.change === 'updated') maybeChanged = true;
    }
  }
  return { deleted, maybeChanged };
}
