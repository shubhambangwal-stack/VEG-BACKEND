// package com.veggofresh.vendor.service;

// import com.veggofresh.platform.exception.BusinessException;
// import com.veggofresh.vendor.entity.InventoryItem;
// import com.veggofresh.vendor.entity.Product;
// import com.veggofresh.vendor.repository.InventoryItemRepository;
// import com.veggofresh.vendor.service.VendorInventoryService;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.Optional;
// import java.util.UUID;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class VendorInventoryServiceTest {

//     @Mock
//     private InventoryItemRepository inventoryItemRepository;

//     @InjectMocks
//     private VendorInventoryService vendorInventoryService;

//     private UUID productId;
//     private InventoryItem inventoryItem;

//     @BeforeEach
//     void setUp() {
//         productId = UUID.randomUUID();
//         Product product = Product.builder().build();
//         product.setId(productId);
        
//         inventoryItem = InventoryItem.builder()
//                 .product(product)
//                 .stockQuantity(10)
//                 .build();
//         inventoryItem.setId(UUID.randomUUID());
//     }

//     @Test
//     void deductStock_success() {
//         when(inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId))
//                 .thenReturn(Optional.of(inventoryItem));

//         vendorInventoryService.deductStock(productId, 3);

//         ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
//         verify(inventoryItemRepository).save(captor.capture());

//         assertEquals(7, captor.getValue().getStockQuantity());
//     }

//     @Test
//     void deductStock_insufficientStock_throwsException() {
//         when(inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId))
//                 .thenReturn(Optional.of(inventoryItem));

//         BusinessException exception = assertThrows(BusinessException.class, () -> {
//             vendorInventoryService.deductStock(productId, 15);
//         });

//         assertEquals("VENDOR_INSUFFICIENT_STOCK", exception.getErrorCode());
//     }
// }
