import {useLayoutEffect, useRef, useState} from 'react';
import {flushSync} from 'react-dom';

// Measure a print-width copy before printing. The screen report remains full-sized.
function PrintPage({children, pageNumber, landscape}) {
  const frame = useRef(null);
  const content = useRef(null);
  const [scale, setScale] = useState(1);
  useLayoutEffect(() => {
    let active = true;
    const fit = () => {
      if (!active || !frame.current || !content.current) return;
      const available = frame.current.clientHeight;
      const availableWidth = frame.current.clientWidth;
      const needed = content.current.scrollHeight;
      const neededWidth = content.current.scrollWidth;
      if (available && availableWidth && needed && neededWidth) {
        const fitScale = Math.min((available - 2) / needed, (availableWidth - 2) / neededWidth);
        setScale(landscape ? Math.min(1, fitScale) : Math.min(1.02, fitScale));
      }
    };
    const observer = new ResizeObserver(fit);
    observer.observe(content.current);
    document.fonts.ready.then(fit);
    const beforePrint = () => flushSync(fit);
    window.addEventListener('beforeprint', beforePrint);
    fit();
    return () => {
      active = false;
      observer.disconnect();
      window.removeEventListener('beforeprint', beforePrint);
    };
  }, [children]);
  return <div ref={frame} className={`report-print-page ${landscape ? 'landscape' : 'portrait'}`} data-page={pageNumber} data-scale={scale}>
    <div ref={content} className="report-print-content" style={{transform: `scale(${scale})`}}>{children}</div>
  </div>;
}

export default function PrintPages({items, perPage = 1, renderItem, landscape = false}) {
  const pages = [];
  for (let i = 0; i < items.length; i += perPage) pages.push(items.slice(i, i + perPage));
  return <div className="report-print-copy" aria-hidden="true">
    {pages.map((page, index) => <PrintPage key={index} pageNumber={index + 1} landscape={landscape}>
      {page.map((item, offset) => renderItem(item, index * perPage + offset))}
    </PrintPage>)}
  </div>;
}
