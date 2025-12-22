import {
  Directive,
  ElementRef,
  Input,
  OnInit,
  OnDestroy,
  Renderer2,
  inject,
  OnChanges,
} from '@angular/core';

@Directive({
  selector: '[jhiLazyLoad]',
  standalone: true,
})
export class LazyLoadImageDirective implements OnInit, OnDestroy, OnChanges {
  @Input('jhiLazyLoad') imageSrc = '';
  @Input() placeholder = 'content/images/placeholder.svg'; // A lightweight placeholder

  private el = inject(ElementRef);
  private renderer = inject(Renderer2);
  private intersectionObserver?: IntersectionObserver;
  private _imageSrc = '';

  ngOnInit(): void {
    this.intersectionObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.loadImage();
            this.intersectionObserver?.unobserve(this.el.nativeElement);
          }
        });
      },
      {
        rootMargin: '50px',
        threshold: 0.01,
      },
    );

    this.intersectionObserver.observe(this.el.nativeElement);
  }

  ngOnChanges(): void {
    if (this.imageSrc && this.imageSrc !== this._imageSrc) {
      this._imageSrc = this.imageSrc;
      // Set placeholder only if the image is not already loaded
      if (this.el.nativeElement.src !== this.imageSrc) {
        this.renderer.setAttribute(
          this.el.nativeElement,
          'src',
          this.placeholder,
        );
        this.renderer.addClass(this.el.nativeElement, 'lazy-loading');
      }
    }
  }

  private loadImage(): void {
    const img = new Image();

    img.onload = () => {
      this.renderer.setAttribute(this.el.nativeElement, 'src', this.imageSrc);
      this.renderer.removeClass(this.el.nativeElement, 'lazy-loading');
      this.renderer.addClass(this.el.nativeElement, 'lazy-loaded');
    };

    img.onerror = () => {
      // In case of an error, fallback to a default product image
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
