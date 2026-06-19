import type { ID } from './common';

export interface AdminMetric {
  label: string;
  value: number;
  trend: number;
}

export interface SensitiveWord {
  id: ID;
  word: string;
  level: 'low' | 'medium' | 'high';
  createdAt: string;
  hitCount: number;
}
