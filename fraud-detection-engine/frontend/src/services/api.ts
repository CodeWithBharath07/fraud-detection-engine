import axios from 'axios';
import { AlertStats, FraudAlert } from '../types';

const BASE = process.env.REACT_APP_API_URL || '/api/v1';

export const fetchAlerts = (page = 0, size = 50, riskLevel?: string): Promise<FraudAlert[]> =>
  axios.get(`${BASE}/alerts`, { params: { page, size, riskLevel } }).then(r => r.data);

export const fetchStats = (): Promise<AlertStats> =>
  axios.get(`${BASE}/alerts/stats`).then(r => r.data);
