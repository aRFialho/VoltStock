'use client';

import Image from 'next/image';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  Activity,
  Boxes,
  ClipboardList,
  Cog,
  FileSpreadsheet,
  Gauge,
  History,
  Layers3,
  LogOut,
  Map,
  PackagePlus,
  QrCode,
  ScanLine,
  ShieldCheck,
  Tags,
  Users
} from 'lucide-react';
import { readSession } from '@/lib/api';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: Gauge },
  { href: '/mapa', label: 'Mapa 3D', icon: Layers3 },
  { href: '/bipagem', label: 'Bipar e Alocar', icon: ScanLine },
  { href: '/produtos', label: 'Produtos', icon: Boxes },
  { href: '/localizacoes', label: 'Localizacoes', icon: Map },
  { href: '/movimentacoes', label: 'Movimentacoes', icon: History },
  { href: '/importar', label: 'Importar', icon: FileSpreadsheet },
  { href: '/etiquetas', label: 'Etiquetas', icon: Tags },
  { href: '/inventario', label: 'Inventario', icon: ClipboardList },
  { href: '/auditoria', label: 'Auditoria', icon: ShieldCheck },
  { href: '/usuarios', label: 'Usuarios', icon: Users },
  { href: '/configuracoes', label: 'Configuracoes', icon: Cog }
];

export function AppShell({ children, title, action }: { children: React.ReactNode; title: string; action?: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = readSession();

  function logout() {
    window.localStorage.removeItem('voltstock.session');
    router.push('/login');
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link href="/dashboard" className="brand-block" aria-label="Volt Stock">
          <Image src="/icon.png" alt="" width={42} height={42} priority />
          <div>
            <strong>Volt Stock</strong>
            <span>Enterprise WMS</span>
          </div>
        </Link>

        <nav className="side-nav" aria-label="Menu principal">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href;
            return (
              <Link key={item.href} href={item.href} className={active ? 'active' : ''} title={item.label}>
                <Icon size={18} aria-hidden="true" />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Volt Stock Operacao</p>
            <h1>{title}</h1>
          </div>
          <div className="topbar-actions">
            {action}
            <div className="session-chip">
              <Activity size={16} aria-hidden="true" />
              <span>{session?.user.role ?? 'offline'}</span>
            </div>
            <button className="icon-button" type="button" onClick={logout} title="Sair">
              <LogOut size={18} aria-hidden="true" />
            </button>
          </div>
        </header>

        {children}
      </main>
    </div>
  );
}
