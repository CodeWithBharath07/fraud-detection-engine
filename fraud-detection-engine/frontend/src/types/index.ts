export interface FraudAlert {
  alertId: string;
  transactionId: string;
  accountId: string;
  amount: number;
  merchant: string;
  country: string;
  fraudulent: boolean;
  fraudProbability: number;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  anomalyReason: string;
  inferenceLatencyMs: number;
  detectedAt: string;
}

export interface AlertStats {
  totalAlerts: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
}
