import { Directive, ElementRef, Input, OnInit, Renderer2, inject, OnChanges } from '@angular/core';

@Directive({
  selector: '[jhiSkeleton]',
  standalone: true,
})
export class SkeletonDirective implements OnInit, OnChanges {
  @Input('jhiSkeleton') isLoading = false;
  @Input() skeletonHeight = '200px';
  @Input() skeletonWidth = '100%';

  private el = inject(ElementRef);
  private renderer = inject(Renderer2);

  ngOnInit(): void {
    this.updateSkeleton();
  }

  ngOnChanges(): void {
    this.updateSkeleton();
  }

  private updateSkeleton(): void {
    if (this.isLoading) {
      this.renderer.addClass(this.el.nativeElement, 'skeleton');
      this.renderer.setStyle(this.el.nativeElement, 'height', this.skeletonHeight);
      this.renderer.setStyle(this.el.nativeElement, 'width', this.skeletonWidth);
    } else {
      this.renderer.removeClass(this.el.nativeElement, 'skeleton');
      this.renderer.removeStyle(this.el.nativeElement, 'height');
      this.renderer.removeStyle(this.el.nativeElement, 'width');
    }
  }
}
