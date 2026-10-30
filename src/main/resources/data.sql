-- Seed products (prices in BDT). ON CONFLICT keeps this safe to re-run on every startup.

INSERT INTO products (id, name, description, category, price, in_stock) VALUES
    (1,  'Bata Comfit Running Shoes',      'Lightweight mesh running shoes with cushioned sole for daily jogging and gym workouts. Available in black and blue, sizes 39-45.', 'shoes', 3499.00, TRUE),
    (2,  'Apex Leather Formal Shoes',      'Genuine leather Oxford formal shoes in brown and black, ideal for office wear and weddings. Sizes 40-44.',                       'shoes', 5990.00, TRUE),
    (3,  'Canvas Casual Sneakers',         'Classic white canvas sneakers with rubber sole, comfortable for everyday casual wear. Unisex, sizes 36-44.',                    'shoes', 1850.00, TRUE),
    (4,  'Kids School Shoes',              'Durable black school shoes for kids with velcro strap and anti-slip sole. Sizes 28-35.',                                       'shoes', 1290.00, FALSE),
    (5,  'Leather Sandals for Men',        'Handmade leather sandals with padded footbed, perfect for summer and Eid. Brown, sizes 39-44.',                                'shoes', 1650.00, TRUE),
    (6,  'Cotton Panjabi',                 'Premium cotton panjabi with embroidered collar for Eid and festive occasions. Colours: white, off-white, navy. Sizes M-XXL.',   'shirts', 2450.00, TRUE),
    (7,  'Slim Fit Formal Shirt',          'Wrinkle-resistant slim fit formal shirt in light blue and white, long sleeves, suitable for office. Sizes S-XL.',              'shirts', 1590.00, TRUE),
    (8,  'Casual Check Shirt',             'Soft cotton flannel check shirt, half sleeves, red and green patterns for casual outings. Sizes M-XL.',                         'shirts', 1190.00, TRUE),
    (9,  'Polo T-Shirt',                   'Breathable pique cotton polo t-shirt with ribbed collar. Colours: black, maroon, grey. Sizes S-XXL.',                           'shirts',  890.00, TRUE),
    (10, 'Linen Summer Shirt',             'Lightweight linen shirt that keeps you cool in hot and humid weather. Beige and sky blue, long sleeves. Sizes M-XL.',          'shirts', 2190.00, FALSE),
    (11, 'Leather Laptop Bag',             'Genuine leather laptop bag fits up to 15.6 inch laptops, with padded compartment and shoulder strap. Dark brown.',             'bags',   6500.00, TRUE),
    (12, 'Waterproof Travel Backpack',     'Waterproof 35 litre backpack with USB charging port, laptop sleeve and multiple pockets, great for travel and monsoon.',       'bags',   3290.00, TRUE),
    (13, 'Jute Tote Bag',                  'Eco-friendly handmade jute tote bag from Bangladesh, strong handles, ideal for shopping and daily use.',                        'bags',    450.00, TRUE),
    (14, 'Ladies Leather Handbag',         'Elegant leather handbag for women with zip closure and detachable strap. Colours: black, tan, red.',                           'bags',   4200.00, TRUE),
    (15, 'Kids School Backpack',           'Lightweight school backpack for kids with cartoon print, water bottle pocket and padded straps.',                              'bags',   1150.00, FALSE)
ON CONFLICT (id) DO NOTHING;
