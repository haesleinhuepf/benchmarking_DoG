
//default sum_image_2d factor1=1f
//default sum_image_2d factor2=1f
__kernel void sum_image_2d(
        __read_only image2d_t input1,
        __read_only image2d_t input2,
        __write_only image2d_t output,
        __private float factor1,
        __private float factor2
)
{
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

    int2 pos = {get_global_id(0), get_global_id(1)};

    float sum = read_imagef(input1, sampler, pos).x * factor1
        + read_imagef(input2, sampler, pos).x * factor2;

    float4 pix = {sum,0,0,0};
	write_imagef(output, pos, pix);
}