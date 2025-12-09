import {
  Directive,
  ElementRef,
  Input,
  OnInit,
  Renderer2,
  inject,
} from '@angular/core';

@Directive({
  selector: '[jhiSkeleton]',
  standalone: true,
})
export class SkeletonDirective implements OnInit {
  @Input('jhiSkeleton') isLoading: boolean = false;
  @Input() skeletonHeight: string = '200px';
  @Input() skeletonWidth: string = '100%';

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
      this.renderer.setStyle(
        this.el.nativeElement,
        'height',
        this.skeletonHeight,
      );
      this.renderer.setStyle(
        this.el.nativeElement,
        'width',
        this.skeletonWidth,
      );
    } else {
      this.renderer.removeClass(this.el.nativeElement, 'skeleton');
      this.renderer.removeStyle(this.el.nativeElement, 'height');
      this.renderer.removeStyle(this.el.nativeElement, 'width');
    }
  }
}
