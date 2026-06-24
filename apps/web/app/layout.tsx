import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Volt Stock',
  description: 'Controle de estoque visual com mapa 2D/3D, QR Code, bipagem e auditoria.',
  icons: {
    icon: '/icon.png',
    apple: '/icon.png'
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
