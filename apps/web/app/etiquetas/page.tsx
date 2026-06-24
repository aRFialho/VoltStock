import { Tags } from 'lucide-react';
import { AppShell } from '@/components/layout/AppShell';
import { ModulePanel } from '@/components/layout/ModulePanel';

export default function EtiquetasPage() {
  return (
    <AppShell title="Etiquetas">
      <ModulePanel
        title="Templates"
        icon={Tags}
        rows={[
          { label: 'Produto', value: 'QR interno + SKU', status: 'PDF planejado' },
          { label: 'Localizacao', value: 'VS-LOC + endereco', status: 'lote planejado' },
          { label: 'Termica', value: 'ZPL/TSPL/ESC-POS', status: 'desktop' }
        ]}
      />
    </AppShell>
  );
}
