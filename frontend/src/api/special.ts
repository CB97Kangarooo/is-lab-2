import { request } from './client';
import type { Hero } from './types';

export function sumImpactSpeed(): Promise<{ value: number }> {
  return request('GET', '/special/impact-speed/sum');
}

export function averageImpactSpeed(): Promise<{ value: number | null }> {
  return request('GET', '/special/impact-speed/average');
}

export function findHeroesByNamePrefix(prefix: string): Promise<Hero[]> {
  return request('GET', '/special/human-beings', { query: { prefix } });
}

export function setAllMoodSorrow(): Promise<{ affected: number }> {
  return request('POST', '/special/mood/sorrow');
}

export function assignLadaKalina(): Promise<{ affected: number }> {
  return request('POST', '/special/cars/lada-kalina');
}
