import { TestBed } from '@angular/core/testing';
import { StatusChip } from './status-chip';

describe('StatusChip', () => {
  it('renders the French label for the given status', () => {
    TestBed.configureTestingModule({ imports: [StatusChip] });
    const fixture = TestBed.createComponent(StatusChip);
    fixture.componentRef.setInput('status', 'PROCESSED');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Traité');
  });
});
