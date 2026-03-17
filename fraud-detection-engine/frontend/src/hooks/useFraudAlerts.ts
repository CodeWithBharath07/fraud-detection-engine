import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { FraudAlert } from '../types';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';
const MAX_ALERTS = 200;

export function useFraudAlerts() {
  const [alerts, setAlerts]       = useState<FraudAlert[]>([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/alerts', (msg) => {
          const alert: FraudAlert = JSON.parse(msg.body);
          setAlerts(prev => [alert, ...prev].slice(0, MAX_ALERTS));
        });
      },
      onDisconnect: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
  }, []);

  useEffect(() => {
    connect();
    return () => { clientRef.current?.deactivate(); };
  }, [connect]);

  const clearAlerts = () => setAlerts([]);

  return { alerts, connected, clearAlerts };
}
