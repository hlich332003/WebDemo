import {
  Directive,
  ElementRef,
  Input,
  OnInit,
  Renderer2,
  inject,
} from '@angular/core';

@Directive({
  selector: '[jhiLazyLoad]',
  standalone: true,
})
export class LazyLoadImageDirective implements OnInit {
  @Input('jhiLazyLoad') imageSrc: string = '';
  @Input() placeholder: string = 'content/images/placeholder.svg';

  private el = inject(ElementRef);
  private renderer = inject(Renderer2);
  private intersectionObserver?: IntersectionObserver;

  ngOnInit(): void {
    // Set placeholder initially
    this.renderer.setAttribute(this.el.nativeElement, 'src', this.placeholder);
    this.renderer.addClass(this.el.nativeElement, 'lazy-loading');

    // Create intersection observer
    this.createIntersectionObserver();
  }

  private createIntersectionObserver(): void {
    const options = {
      root: null, // viewport
      rootMargin: '50px', // Load image 50px before it enters viewport
      threshold: 0.01,
    };

    this.intersectionObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          this.loadImage();
          this.intersectionObserver?.unobserve(this.el.nativeElement);
        }
      });
    }, options);

    this.intersectionObserver.observe(this.el.nativeElement);
  }

  private loadImage(): void {
    const img = new Image();

    img.onload = () => {
      this.renderer.setAttribute(this.el.nativeElement, 'src', this.imageSrc);
      this.renderer.removeClass(this.el.nativeElement, 'lazy-loading');
      this.renderer.addClass(this.el.nativeElement, 'lazy-loaded');
    };

    img.onerror = () => {
      this.renderer.setAttribute(
        this.el.nativeElement,
        'src',
        'content/images/default-product.svg',
      );
      this.renderer.removeClass(this.el.nativeElement, 'lazy-loading');
      this.renderer.addClass(this.el.nativeElement, 'lazy-error');
    };

    img.src = this.imageSrc;
  }

  ngOnDestroy(): void {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
  }
}
