import React, { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { OverviewPage } from '../../pages/OverviewPage';
import { TransactionExplorerPage } from '../../pages/TransactionExplorerPage';
import { NetworkAnalysisPage } from '../../pages/NetworkAnalysisPage';
import { DetectionEventsPage } from '../../pages/DetectionEventsPage';
import { EvaluationLabPage } from '../../pages/EvaluationLabPage';
import { SystemHealthPage } from '../../pages/SystemHealthPage';
import { SettingsPage } from '../../pages/SettingsPage';
import type { NavigationTab, SystemTelemetry, Transaction } from '../../types';
import { fetchSystemTelemetry } from '../../services/api';
import { DEMO_TRANSACTIONS, DEMO_DETECTION_EVENTS, getDemoGraphElements } from '../../services/demoFixture';

export const AppShell: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<NavigationTab>('overview');
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  const [telemetry, setTelemetry] = useState<SystemTelemetry>({
    backendHealth: 'CONNECTING',
    neo4jStatus: 'UNKNOWN',
    kafkaTopicStatus: 'UNKNOWN',
    recordsConsumedTotal: 0,
    cycleDetectionCount: 0,
    cycleDetectionMaxDelaySec: 0,
    cycleDetectionTotalTimeSec: 0,
    lastSuccessfulPoll: null,
    pollingError: null,
    responseTimeMs: 0
  });

  const [isRefreshing, setIsRefreshing] = useState(false);

  // Fixture and Graph Data
  const graphElements = getDemoGraphElements();

  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    try {
      const data = await fetchSystemTelemetry();
      setTelemetry(data);
    } catch (err: unknown) {
      console.error('Telemetry refresh failed:', err);
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  // Periodic Telemetry Polling (every 5 seconds)
  useEffect(() => {
    handleRefresh();
    const interval = setInterval(handleRefresh, 5000);
    return () => clearInterval(interval);
  }, [handleRefresh]);

  const renderActivePage = () => {
    switch (currentTab) {
      case 'overview':
        return (
          <OverviewPage
            telemetry={telemetry}
            transactions={DEMO_TRANSACTIONS}
            events={DEMO_DETECTION_EVENTS}
            graphElements={graphElements}
            onNavigateTab={setCurrentTab}
            onSelectTransaction={(tx) => {
              setSelectedTx(tx);
              setCurrentTab('explorer');
            }}
          />
        );
      case 'explorer':
        return (
          <TransactionExplorerPage
            transactions={DEMO_TRANSACTIONS}
            selectedTx={selectedTx}
            onSelectTx={setSelectedTx}
          />
        );
      case 'network':
        return (
          <NetworkAnalysisPage
            graphElements={graphElements}
          />
        );
      case 'events':
        return (
          <DetectionEventsPage
            events={DEMO_DETECTION_EVENTS}
            onNavigateToNetwork={() => setCurrentTab('network')}
          />
        );
      case 'evaluation':
        return <EvaluationLabPage />;
      case 'health':
        return (
          <SystemHealthPage
            telemetry={telemetry}
            onRefresh={handleRefresh}
            isRefreshing={isRefreshing}
          />
        );
      case 'settings':
        return <SettingsPage />;
      default:
        return null;
    }
  };

  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        isCollapsed={isSidebarCollapsed}
        onToggleCollapse={() => setIsSidebarCollapsed(c => !c)}
        cycleCount={DEMO_DETECTION_EVENTS.length}
      />

      <div className="main-content-area">
        <TopBar
          currentTab={currentTab}
          telemetry={telemetry}
          onRefresh={handleRefresh}
          isRefreshing={isRefreshing}
        />

        <main className="page-scroll-container">
          {renderActivePage()}
        </main>
      </div>
    </div>
  );
};
