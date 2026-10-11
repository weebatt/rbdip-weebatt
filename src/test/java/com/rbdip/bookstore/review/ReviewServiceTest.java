package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long PRODUCT_ID = 5L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PurchaseVerification purchaseVerification;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, purchaseVerification);
    }

    @Test
    void createsAnonymousReviewThroughModulePort() {
        Review saved = new Review(PRODUCT_ID, "anonymous", 5, "Great");
        when(purchaseVerification.hasPurchaseForProduct(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any(Review.class))).thenReturn(saved);

        Review result = reviewService.addReview(PRODUCT_ID, null, 5, "Great");

        assertThat(result).isSameAs(saved);
        verify(purchaseVerification).hasPurchaseForProduct(PRODUCT_ID);
    }

    @Test
    void listsReviewsForRequestedProduct() {
        List<Review> reviews = List.of(new Review(PRODUCT_ID, "Anna", 4, null));
        when(reviewRepository.findByProductId(PRODUCT_ID)).thenReturn(reviews);

        assertThat(reviewService.listReviews(PRODUCT_ID)).isSameAs(reviews);
    }
}
