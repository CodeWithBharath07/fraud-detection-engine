import React, { useEffect, useState } from 'react';
import {
  Box, Container, Typography, Chip, Card, CardContent,
  Grid, Alert, Badge, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, CircularProgress,
  IconButton, Tooltip, LinearProgress
} from '@mui/material';
import {
  Warning, Error, CheckCircle, Info, Bolt,
  WifiOff, Wifi, DeleteSweep, TrendingUp
} from '@mui/icons-material';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip as RechartTooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend
} from 'recharts';
import { useFraudAlerts } from './hooks/useFraudAlerts';
import { fetchStats } from './services/api';
import { FraudAlert, AlertStats } from './types';

const RISK_COLORS: Record<string, string> = {
  CRITICAL: '#d32f2f',
  HIGH:     '#f44336',
  MEDIUM:   '#ff9800',
  LOW:      '#4caf50',
};

const RiskChip: React.FC<{ level: string }> = ({ level }) => (
  <Chip
    label={level}
    size="small"
    icon={level === 'CRITICAL' || level === 'HIGH' ? <Error fontSize="small" /> : <Warning fontSize="small" />}
    style={{ background: RISK_COLORS[level], color: '#fff', fontWeight: 700 }}
  />
);

const StatCard: React.FC<{ label: string; value: number; color: string; icon: React.ReactNode }> = ({ label, value, color, icon }) => (
  <Card sx={{ p: 2, borderLeft: `4px solid ${color}`, height: '100%' }}>
    <Box display="flex" alignItems="center" justifyContent="space-between">
      <Box>
        <Typography variant="h4" fontWeight={800} sx={{ color }}>{value.toLocaleString()}</Typography>
        <Typography variant="body2" color="text.secondary">{label}</Typography>
      </Box>
      <Box sx={{ color, opacity: 0.7, fontSize: 40 }}>{icon}</Box>
    </Box>
  </Card>
);

export default function App() {
  const { alerts, connected, clearAlerts } = useFraudAlerts();
  const [stats, setStats]   = useState<AlertStats | null>(null);
  const [chartData, setChartData] = useState<{ time: string; count: number }[]>([]);

  // Poll stats every 10s
  useEffect(() => {
    const load = () => fetchStats().then(setStats).catch(() => {});
    load();
    const t = setInterval(load, 10_000);
    return () => clearInterval(t);
  }, []);

  // Build rolling 60s chart from incoming alerts
  useEffect(() => {
    if (alerts.length === 0) return;
    const now   = new Date();
    const label = now.toLocaleTimeString();
    setChartData(prev => [...prev.slice(-29), { time: label, count: alerts.filter(a => {
      const age = (now.getTime() - new Date(a.detectedAt).getTime()) / 1000;
      return age < 5;
    }).length }]);
  }, [alerts]);

  const pieData = stats ? [
    { name: 'Critical', value: stats.criticalCount, color: '#d32f2f' },
    { name: 'High',     value: stats.highCount,     color: '#f44336' },
    { name: 'Medium',   value: stats.mediumCount,   color: '#ff9800' },
    { name: 'Low',      value: stats.lowCount,      color: '#4caf50' },
  ].filter(d => d.value > 0) : [];

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#0f0f1a', color: '#fff' }}>
      {/* Header */}
      <Box sx={{ bgcolor: '#1a1a2e', borderBottom: '1px solid #2a2a4a', py: 2, px: 3 }}>
        <Box display="flex" alignItems="center" gap={2}>
          <Bolt sx={{ color: '#f44336', fontSize: 32 }} />
          <Box>
            <Typography variant="h5" fontWeight={800} letterSpacing={-0.5}>
              Real-Time Fraud Detection
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.6 }}>
              Apache Kafka · Scikit-learn · Sub-100ms alert latency
            </Typography>
          </Box>
          <Box sx={{ ml: 'auto', display: 'flex', alignItems: 'center', gap: 1 }}>
            {connected
              ? <Chip icon={<Wifi fontSize="small" />} label="Live" size="small" sx={{ bgcolor: '#1b5e20', color: '#fff' }} />
              : <Chip icon={<WifiOff fontSize="small" />} label="Disconnected" size="small" color="error" />}
            <Tooltip title="Clear alerts">
              <IconButton onClick={clearAlerts} size="small" sx={{ color: '#888' }}>
                <DeleteSweep />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
      </Box>

      <Container maxWidth="xl" sx={{ py: 3 }}>
        {/* Stat Cards */}
        <Grid container spacing={2} mb={3}>
          <Grid item xs={6} sm={3}>
            <StatCard label="Total Alerts" value={stats?.totalAlerts ?? 0} color="#7c3aed" icon={<TrendingUp />} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard label="Critical" value={stats?.criticalCount ?? 0} color="#d32f2f" icon={<Error />} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard label="High" value={stats?.highCount ?? 0} color="#f44336" icon={<Warning />} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard label="Medium / Low" value={(stats?.mediumCount ?? 0) + (stats?.lowCount ?? 0)} color="#ff9800" icon={<Info />} />
          </Grid>
        </Grid>

        <Grid container spacing={3} mb={3}>
          {/* Live Alert Feed Chart */}
          <Grid item xs={12} md={8}>
            <Card sx={{ bgcolor: '#1a1a2e', p: 2 }}>
              <Typography variant="subtitle1" fontWeight={700} mb={2} sx={{ color: '#ccc' }}>
                Live Alert Rate (last 30 intervals)
              </Typography>
              <ResponsiveContainer width="100%" height={180}>
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="alertGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"  stopColor="#f44336" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#f44336" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#2a2a4a" />
                  <XAxis dataKey="time" tick={{ fill: '#666', fontSize: 10 }} />
                  <YAxis tick={{ fill: '#666', fontSize: 10 }} />
                  <RechartTooltip contentStyle={{ background: '#1a1a2e', border: '1px solid #2a2a4a' }} />
                  <Area type="monotone" dataKey="count" stroke="#f44336" fill="url(#alertGrad)" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </Card>
          </Grid>

          {/* Risk Breakdown Pie */}
          <Grid item xs={12} md={4}>
            <Card sx={{ bgcolor: '#1a1a2e', p: 2, height: '100%' }}>
              <Typography variant="subtitle1" fontWeight={700} mb={1} sx={{ color: '#ccc' }}>
                Risk Distribution
              </Typography>
              {pieData.length > 0 ? (
                <ResponsiveContainer width="100%" height={180}>
                  <PieChart>
                    <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={75} dataKey="value" paddingAngle={3}>
                      {pieData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                    </Pie>
                    <Legend wrapperStyle={{ color: '#aaa', fontSize: 12 }} />
                    <RechartTooltip contentStyle={{ background: '#1a1a2e', border: '1px solid #2a2a4a' }} />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height={180}>
                  <Typography color="text.secondary" variant="body2">Waiting for data...</Typography>
                </Box>
              )}
            </Card>
          </Grid>
        </Grid>

        {/* Live Alert Table */}
        <Card sx={{ bgcolor: '#1a1a2e' }}>
          <Box sx={{ p: 2, borderBottom: '1px solid #2a2a4a', display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#ccc' }}>
              Live Fraud Alerts
            </Typography>
            <Badge badgeContent={alerts.length} color="error" sx={{ ml: 1 }} />
            {connected && <LinearProgress sx={{ flex: 1, ml: 2, height: 2, bgcolor: '#2a2a4a' }} color="error" />}
          </Box>

          {alerts.length === 0 ? (
            <Box sx={{ p: 6, textAlign: 'center' }}>
              <Bolt sx={{ fontSize: 48, opacity: 0.2, color: '#f44336' }} />
              <Typography color="text.secondary" mt={1}>
                {connected ? 'Waiting for transactions...' : 'Connecting to WebSocket...'}
              </Typography>
            </Box>
          ) : (
            <TableContainer sx={{ maxHeight: 440 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow sx={{ '& th': { bgcolor: '#12122a', color: '#888', fontWeight: 700 } }}>
                    <TableCell>Risk</TableCell>
                    <TableCell>Transaction ID</TableCell>
                    <TableCell>Account</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Merchant</TableCell>
                    <TableCell>Country</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell align="right">Prob</TableCell>
                    <TableCell align="right">Latency</TableCell>
                    <TableCell>Time</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {alerts.map((alert) => (
                    <TableRow
                      key={alert.alertId}
                      sx={{
                        '& td': { color: '#ccc', fontSize: '0.78rem', borderColor: '#2a2a4a' },
                        bgcolor: alert.riskLevel === 'CRITICAL' ? 'rgba(211,47,47,0.08)' : 'transparent',
                        '&:hover': { bgcolor: '#1f1f38' }
                      }}
                    >
                      <TableCell><RiskChip level={alert.riskLevel} /></TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.7rem !important' }}>
                        {alert.transactionId.slice(0, 8)}...
                      </TableCell>
                      <TableCell>{alert.accountId}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700, color: '#f44336 !important' }}>
                        ${alert.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                      </TableCell>
                      <TableCell>{alert.merchant}</TableCell>
                      <TableCell>{alert.country}</TableCell>
                      <TableCell sx={{ fontSize: '0.7rem !important', color: '#aaa !important' }}>
                        {alert.anomalyReason?.replace(/_/g, ' ')}
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="caption" sx={{ color: RISK_COLORS[alert.riskLevel] }}>
                          {(alert.fraudProbability * 100).toFixed(0)}%
                        </Typography>
                      </TableCell>
                      <TableCell align="right" sx={{ color: '#888 !important' }}>
                        {alert.inferenceLatencyMs}ms
                      </TableCell>
                      <TableCell sx={{ color: '#666 !important', fontSize: '0.7rem !important' }}>
                        {new Date(alert.detectedAt).toLocaleTimeString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Card>
      </Container>
    </Box>
  );
}
